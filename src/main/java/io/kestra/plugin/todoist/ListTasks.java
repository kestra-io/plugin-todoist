package io.kestra.plugin.todoist;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List tasks from Todoist",
    description = "Retrieves a list of tasks from Todoist with optional project ID or filter query"
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "List all active tasks",
            code = """
                id: todoist_list_tasks
                namespace: company.team

                tasks:
                  - id: list_tasks
                    type: io.kestra.plugin.todoist.ListTasks
                    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
                """
        ),
        @Example(
            full = true,
            title = "List tasks for a specific project",
            code = """
                id: todoist_list_project_tasks
                namespace: company.team

                tasks:
                  - id: list_project_tasks
                    type: io.kestra.plugin.todoist.ListTasks
                    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
                    projectId: "2203306141"
                    fetchType: FETCH
                """
        ),
        @Example(
            full = true,
            title = "Store tasks in internal storage for large datasets",
            code = """
                id: todoist_store_tasks
                namespace: company.team

                tasks:
                  - id: store_tasks
                    type: io.kestra.plugin.todoist.ListTasks
                    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
                    fetchType: STORE
                """
        ),
        @Example(
            full = true,
            title = "List tasks using a filter",
            code = """
                id: todoist_list_filtered_tasks
                namespace: company.team

                tasks:
                  - id: list_filtered_tasks
                    type: io.kestra.plugin.todoist.ListTasks
                    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
                    filter: "today"
                    fetchType: FETCH
                """
        ),
        @Example(
            full = true,
            title = "List tasks with custom limit",
            code = """
                id: todoist_list_tasks_with_limit
                namespace: company.team

                tasks:
                  - id: list_tasks_limit
                    type: io.kestra.plugin.todoist.ListTasks
                    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
                    limit: 100
                    fetchType: FETCH
                """
        )
    }
)
public class ListTasks extends AbstractTodoistTask implements RunnableTask<ListTasks.Output> {

    @Schema(
        title = "Project ID",
        description = "Filter tasks by project ID. Cannot be used together with filter parameter."
    )
    private Property<String> projectId;

    @Schema(
        title = "Filter",
        description = "Custom filter query (e.g., \"today\", \"overdue\", \"priority 1\"). Cannot be used together with projectId parameter."
    )
    private Property<String> filter;

    @Schema(
        title = "Limit",
        description = "Maximum number of tasks to return per page. If not set, all tasks will be fetched by automatically paginating through all results. If set, only that many tasks will be returned (defaults to 50 per page). Both /api/v1/tasks and /api/v1/tasks/filter endpoints support this parameter."
    )
    private Property<Integer> limit;

    @Schema(
        title = "Fetch Type",
        description = "The way to fetch data: FETCH_ONE (first task only), FETCH (all tasks in memory), or STORE (store in internal storage for large datasets)"
    )
    @Builder.Default
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String rToken = runContext.render(apiToken).as(String.class).orElseThrow();

        String rFilter = runContext.render(filter).as(String.class).orElse(null);
        String rProjectId = runContext.render(projectId).as(String.class).orElse(null);
        Integer rLimit = runContext.render(limit).as(Integer.class).orElse(null);

        // Validate that filter and projectId are not both provided
        if (rFilter != null && rProjectId != null) {
            throw new IllegalArgumentException("Cannot use both 'filter' and 'projectId' parameters together. Please use only one.");
        }

        // Accumulate all tasks from all pages
        List<Map<String, Object>> allTasks = new java.util.ArrayList<>();
        String cursor = null;
        boolean fetchAll = (rLimit == null); // If limit is not set, fetch all pages
        
        do {
            String url = buildUrl(rFilter, rProjectId, rLimit, cursor);
            
            HttpRequest request = createRequestBuilder(rToken, url)
                .method("GET")
                .build();

            HttpResponse<String> response = sendRequest(runContext, request);

            if (response.getStatus().getCode() >= 400) {
                throw new Exception("Failed to list tasks: " + response.getStatus().getCode() + " - " + response.getBody());
            }

            // Parse response to get tasks and next cursor
            String responseBody = response.getBody();
            Map<String, Object> responseMap;
            try {
                responseMap = JacksonMapper.ofJson().readValue(responseBody, Map.class);
            } catch (Exception e) {
                logger.error("Failed to parse response: {}", responseBody, e);
                throw new Exception("Failed to parse tasks response: " + e.getMessage() + ". Response: " + responseBody, e);
            }

            // Extract tasks from response
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pageTasks = extractTasksFromResponse(responseMap, responseBody, logger);
            
            if (pageTasks != null) {
                allTasks.addAll(pageTasks);
            }

            // Get next cursor for pagination
            Object nextCursorObj = responseMap.get("next_cursor");
            cursor = (nextCursorObj != null && !nextCursorObj.toString().isEmpty()) ? nextCursorObj.toString() : null;
            
            // If limit is set, only fetch one page
            if (!fetchAll) {
                break;
            }
            
            // Log progress if fetching all pages
            if (cursor != null) {
                logger.debug("Fetched {} tasks so far, continuing pagination...", allTasks.size());
            }
            
        } while (cursor != null);

        logger.info("Retrieved {} tasks{}", allTasks.size(), fetchAll ? " (all pages)" : "");

        FetchType renderedFetchType = runContext.render(fetchType).as(FetchType.class).orElse(FetchType.FETCH);
        Output.OutputBuilder outputBuilder = Output.builder();

        switch (renderedFetchType) {
            case FETCH_ONE -> {
                if (!allTasks.isEmpty()) {
                    outputBuilder.row(allTasks.get(0)).size(1L);
                } else {
                    outputBuilder.size(0L);
                }
            }
            case STORE -> {
                File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
                try (var fileOutputStream = new java.io.FileOutputStream(tempFile)) {
                    for (Map<String, Object> task : allTasks) {
                        FileSerde.write(fileOutputStream, task);
                    }
                }
                URI uri = runContext.storage().putFile(tempFile);
                outputBuilder.uri(uri).size((long) allTasks.size());
            }
            case FETCH -> {
                outputBuilder.rows(allTasks).size((long) allTasks.size());
            }
        }

        return outputBuilder.build();
    }

    /**
     * Builds the URL for the API request with appropriate query parameters
     */
    private String buildUrl(String filter, String projectId, Integer limit, String cursor) {
        StringBuilder urlBuilder;
        
        if (filter != null) {
            // Use the filter endpoint: /api/v1/tasks/filter
            // API v1 uses 'query' parameter, not 'filter'
            urlBuilder = new StringBuilder(BASE_URL + "/tasks/filter?query=" + URLEncoder.encode(filter, StandardCharsets.UTF_8));
            if (limit != null) {
                urlBuilder.append("&limit=").append(limit);
            }
            if (cursor != null) {
                urlBuilder.append("&cursor=").append(URLEncoder.encode(cursor, StandardCharsets.UTF_8));
            }
        } else {
            // Use the standard tasks endpoint: /api/v1/tasks
            urlBuilder = new StringBuilder(BASE_URL + "/tasks");
            boolean hasParams = false;
            if (projectId != null) {
                urlBuilder.append("?project_id=").append(projectId);
                hasParams = true;
            }
            if (limit != null) {
                urlBuilder.append(hasParams ? "&" : "?").append("limit=").append(limit);
                hasParams = true;
            }
            if (cursor != null) {
                urlBuilder.append(hasParams ? "&" : "?").append("cursor=").append(URLEncoder.encode(cursor, StandardCharsets.UTF_8));
            }
        }
        
        return urlBuilder.toString();
    }

    /**
     * Extracts tasks from the API response, handling different response formats
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractTasksFromResponse(Map<String, Object> responseMap, String responseBody, Logger logger) throws Exception {
        List<Map<String, Object>> tasks;
        
        try {
            if (responseMap.containsKey("results") && responseMap.get("results") instanceof List) {
                // Paginated response with 'results' array (API v1 standard)
                tasks = (List<Map<String, Object>>) responseMap.get("results");
            } else if (responseMap.containsKey("items") && responseMap.get("items") instanceof List) {
                // Fallback for 'items' array
                tasks = (List<Map<String, Object>>) responseMap.get("items");
            } else if (responseMap.containsKey("data") && responseMap.get("data") instanceof List) {
                // Fallback for 'data' array
                tasks = (List<Map<String, Object>>) responseMap.get("data");
            } else {
                // If it's an object but no results/items/data, try as direct array
                tasks = JacksonMapper.ofJson().readValue(responseBody, List.class);
            }
        } catch (com.fasterxml.jackson.databind.exc.MismatchedInputException e) {
            // If parsing as object fails (not an object), try as direct array
            try {
                tasks = JacksonMapper.ofJson().readValue(responseBody, List.class);
            } catch (Exception e2) {
                logger.error("Failed to parse response as object or array. Response: {}", responseBody);
                throw new Exception("Failed to parse tasks response: " + e2.getMessage() + ". Response: " + responseBody, e2);
            }
        } catch (Exception e) {
            logger.error("Unexpected error parsing response: {}", responseBody, e);
            throw new Exception("Failed to parse tasks response: " + e.getMessage() + ". Response: " + responseBody, e);
        }
        
        return tasks != null ? tasks : new java.util.ArrayList<>();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Row",
            description = "Single task when fetchType is FETCH_ONE"
        )
        private final Map<String, Object> row;

        @Schema(
            title = "Rows",
            description = "List of tasks when fetchType is FETCH"
        )
        private final List<Map<String, Object>> rows;

        @Schema(
            title = "URI",
            description = "URI of the stored file when fetchType is STORE"
        )
        private final URI uri;

        @Schema(
            title = "Size",
            description = "Number of tasks retrieved"
        )
        private final Long size;
    }
}

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
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List tasks from Todoist",
    description = "Retrieves a list of tasks from Todoist with optional project filter"
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
        )
    }
)
public class ListTasks extends AbstractTodoistTask implements RunnableTask<ListTasks.Output> {
    
    @Schema(
        title = "Project ID",
        description = "Filter tasks by project ID"
    )
    private Property<String> projectId;

    @Schema(
        title = "Fetch Type",
        description = "The way to fetch data: FETCH_ONE (first task only), FETCH (all tasks in memory), or STORE (store in internal storage for large datasets)"
    )
    @Builder.Default
    private Property<FetchType> fetchType = Property.of(FetchType.FETCH);

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String rToken = runContext.render(apiToken).as(String.class).orElseThrow();
        
        StringBuilder urlBuilder = new StringBuilder(BASE_URL + "/tasks");
        
        runContext.render(projectId).as(String.class).ifPresent(p -> {
            urlBuilder.append("?project_id=").append(p);
        });
        
        String url = urlBuilder.toString();
        
        HttpRequest request = createRequestBuilder(rToken, url)
            .method("GET")
            .build();
        
        HttpResponse<String> response = sendRequest(runContext, request);
        
        if (response.getStatus().getCode() >= 400) {
            throw new Exception("Failed to list tasks: " + response.getStatus().getCode() + " - " + response.getBody());
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = JacksonMapper.ofJson().readValue(response.getBody(), List.class);
        
        logger.info("Retrieved {} tasks", tasks.size());
        
        FetchType renderedFetchType = runContext.render(fetchType).as(FetchType.class).orElse(FetchType.FETCH);
        Output.OutputBuilder outputBuilder = Output.builder();
        
        switch (renderedFetchType) {
            case FETCH_ONE -> {
                if (!tasks.isEmpty()) {
                    outputBuilder.row(tasks.get(0)).size(1L);
                } else {
                    outputBuilder.size(0L);
                }
            }
            case STORE -> {
                File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
                try (var fileOutputStream = new java.io.FileOutputStream(tempFile)) {
                    for (Map<String, Object> task : tasks) {
                        FileSerde.write(fileOutputStream, task);
                    }
                }
                URI uri = runContext.storage().putFile(tempFile);
                outputBuilder.uri(uri).size((long) tasks.size());
            }
            case FETCH -> {
                outputBuilder.rows(tasks).size((long) tasks.size());
            }
        }
        
        return outputBuilder.build();
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

package io.kestra.plugin.todoist;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import okhttp3.*;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List tasks from Todoist",
    description = "Retrieves a list of tasks from Todoist with optional filters"
)
@Plugin(
    examples = {
        @Example(
            title = "List all active tasks",
            code = {
                "apiToken: \"{{ secret('TODOIST_API_TOKEN') }}\""
            }
        ),
        @Example(
            title = "List tasks for a specific project",
            code = {
                "apiToken: \"{{ secret('TODOIST_API_TOKEN') }}\"",
                "projectId: \"2203306141\""
            }
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
        title = "Filter",
        description = "Filter tasks by any supported filter (e.g., 'today', 'overdue', 'priority 1')"
    )
    private Property<String> filter;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String token = runContext.render(apiToken).as(String.class).orElseThrow();
        
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "/tasks").newBuilder();
        
        runContext.render(projectId).as(String.class).ifPresent(p -> urlBuilder.addQueryParameter("project_id", p));
        runContext.render(filter).as(String.class).ifPresent(f -> urlBuilder.addQueryParameter("filter", f));
        
        OkHttpClient client = createHttpClient();
        Request request = createRequestBuilder(token)
            .url(urlBuilder.build())
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                throw new Exception("Failed to list tasks: " + response.code() + " - " + responseBody);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tasks = mapper.readValue(responseBody, List.class);
            
            logger.info("Retrieved {} tasks", tasks.size());
            
            return Output.builder()
                .tasks(tasks)
                .count(tasks.size())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Tasks",
            description = "List of tasks retrieved from Todoist"
        )
        private final List<Map<String, Object>> tasks;
        
        @Schema(
            title = "Count",
            description = "Number of tasks retrieved"
        )
        private final Integer count;
    }
}

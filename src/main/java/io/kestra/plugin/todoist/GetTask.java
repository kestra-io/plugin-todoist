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

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get a task from Todoist",
    description = "Retrieves details of a specific task by ID"
)
@Plugin(
    examples = {
        @Example(
            title = "Get a task by ID",
            code = {
                "apiToken: \"{{ secret('TODOIST_API_TOKEN') }}\"",
                "taskId: \"7498765432\""
            }
        )
    }
)
public class GetTask extends AbstractTodoistTask implements RunnableTask<GetTask.Output> {
    
    @Schema(
        title = "Task ID",
        description = "The ID of the task to retrieve"
    )
    private Property<String> taskId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String token = runContext.render(apiToken).as(String.class).orElseThrow();
        String id = runContext.render(taskId).as(String.class).orElseThrow();
        
        OkHttpClient client = createHttpClient();
        Request request = createRequestBuilder(token)
            .url(BASE_URL + "/tasks/" + id)
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                throw new Exception("Failed to get task: " + response.code() + " - " + responseBody);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> task = mapper.readValue(responseBody, Map.class);
            
            logger.info("Retrieved task: {}", task.get("content"));
            
            return Output.builder()
                .task(task)
                .taskId(task.get("id").toString())
                .content(task.get("content").toString())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Task",
            description = "The complete task object from Todoist"
        )
        private final Map<String, Object> task;
        
        @Schema(
            title = "Task ID",
            description = "The ID of the task"
        )
        private final String taskId;
        
        @Schema(
            title = "Content",
            description = "The content/title of the task"
        )
        private final String content;
    }
}

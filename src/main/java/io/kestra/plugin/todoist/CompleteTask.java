package io.kestra.plugin.todoist;

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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Complete a task in Todoist",
    description = "Marks a task as completed in Todoist"
)
@Plugin(
    examples = {
        @Example(
            title = "Complete a task by ID",
            code = {
                "apiToken: \"{{ secret('TODOIST_API_TOKEN') }}\"",
                "taskId: \"7498765432\""
            }
        )
    }
)
public class CompleteTask extends AbstractTodoistTask implements RunnableTask<CompleteTask.Output> {
    
    @Schema(
        title = "Task ID",
        description = "The ID of the task to complete"
    )
    private Property<String> taskId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String token = runContext.render(apiToken).as(String.class).orElseThrow();
        String id = runContext.render(taskId).as(String.class).orElseThrow();
        
        OkHttpClient client = createHttpClient();
        Request request = createRequestBuilder(token)
            .url(BASE_URL + "/tasks/" + id + "/close")
            .post(RequestBody.create("", null))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                throw new Exception("Failed to complete task: " + response.code() + " - " + responseBody);
            }
            
            logger.info("Task {} completed successfully", id);
            
            return Output.builder()
                .taskId(id)
                .success(true)
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Task ID",
            description = "The ID of the completed task"
        )
        private final String taskId;
        
        @Schema(
            title = "Success",
            description = "Whether the task was successfully completed"
        )
        private final Boolean success;
    }
}

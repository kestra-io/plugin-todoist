package io.kestra.plugin.todoist;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a task in Todoist",
    description = "Permanently deletes a task from Todoist"
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a task by ID",
            code = {
                "apiToken: \"{{ secret('TODOIST_API_TOKEN') }}\"",
                "taskId: \"7498765432\""
            }
        )
    }
)
public class DeleteTask extends AbstractTodoistTask implements RunnableTask<VoidOutput> {
    
    @Schema(
        title = "Task ID",
        description = "The ID of the task to delete"
    )
    @NotNull
    private Property<String> taskId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String rToken = runContext.render(apiToken).as(String.class).orElseThrow();
        String rTaskId = runContext.render(taskId).as(String.class).orElseThrow();
        
        HttpRequest request = createRequestBuilder(rToken, BASE_URL + "/tasks/" + rTaskId)
            .method("DELETE")
            .build();
        
        HttpResponse<String> response = sendRequest(runContext, request);
        
        if (response.getStatus().getCode() >= 400) {
            throw new Exception("Failed to delete task: " + response.getStatus().getCode() + " - " + response.getBody());
        }
        
        logger.info("Task {} deleted successfully", rTaskId);
        
        return null;
    }
}

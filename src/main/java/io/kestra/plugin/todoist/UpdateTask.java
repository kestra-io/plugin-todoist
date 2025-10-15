package io.kestra.plugin.todoist;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Update an existing task in Todoist",
    description = "Updates an existing task in Todoist with new values"
)
@Plugin(
    examples = {
        @Example(
            title = "Update task content",
            code = {
                "apiToken: \"{{ secret('TODOIST_API_TOKEN') }}\"",
                "taskId: \"7498765432\"",
                "content: \"Updated task title\""
            }
        ),
        @Example(
            title = "Update task priority and due date",
            code = {
                "apiToken: \"{{ secret('TODOIST_API_TOKEN') }}\"",
                "taskId: \"7498765432\"",
                "priority: 4",
                "dueString: \"tomorrow\""
            }
        )
    }
)
public class UpdateTask extends AbstractTodoistTask implements RunnableTask<UpdateTask.Output> {
    
    @Schema(
        title = "Task ID",
        description = "The ID of the task to update"
    )
    @NotNull
    private Property<String> taskId;
    
    @Schema(
        title = "Task content",
        description = "The new content/title of the task"
    )
    private Property<String> content;
    
    @Schema(
        title = "Task description",
        description = "The new description for the task"
    )
    private Property<String> taskDescription;
    
    @Schema(
        title = "Priority",
        description = "Task priority from 1 (normal) to 4 (urgent)"
    )
    private Property<Integer> priority;
    
    @Schema(
        title = "Due string",
        description = "Human-defined task due date (e.g., 'tomorrow', 'next Monday', '2025-12-31')"
    )
    private Property<String> dueString;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String rToken = runContext.render(apiToken).as(String.class).orElseThrow();
        String rTaskId = runContext.render(taskId).as(String.class).orElseThrow();
        
        Map<String, Object> requestBody = new HashMap<>();
        
        runContext.render(content).as(String.class).ifPresent(c -> requestBody.put("content", c));
        runContext.render(taskDescription).as(String.class).ifPresent(d -> requestBody.put("description", d));
        runContext.render(priority).as(Integer.class).ifPresent(p -> requestBody.put("priority", p));
        runContext.render(dueString).as(String.class).ifPresent(d -> requestBody.put("due_string", d));
        
        if (requestBody.isEmpty()) {
            throw new IllegalArgumentException("At least one field must be provided to update");
        }
        
        String jsonBody = JacksonMapper.ofJson().writeValueAsString(requestBody);
        
        HttpRequest request = createRequestBuilder(rToken, BASE_URL + "/tasks/" + rTaskId)
            .method("POST")
            .body(HttpRequest.StringRequestBody.builder().content(jsonBody).build())
            .build();
        
        HttpResponse<String> response = sendRequest(runContext, request);
        
        if (response.getStatus().getCode() >= 400) {
            throw new Exception("Failed to update task: " + response.getStatus().getCode() + " - " + response.getBody());
        }
        
        logger.info("Task {} updated successfully", rTaskId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = JacksonMapper.ofJson().readValue(response.getBody(), Map.class);
        
        return Output.builder()
            .taskId(result.get("id").toString())
            .content(result.get("content").toString())
            .url(result.get("url").toString())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Task ID",
            description = "The ID of the updated task"
        )
        private final String taskId;
        
        @Schema(
            title = "Task content",
            description = "The updated content of the task"
        )
        private final String content;
        
        @Schema(
            title = "Task URL",
            description = "The URL to view the task in Todoist"
        )
        private final String url;
    }
}

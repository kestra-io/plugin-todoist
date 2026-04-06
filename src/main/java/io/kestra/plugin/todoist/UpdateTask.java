package io.kestra.plugin.todoist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

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
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Update Todoist task fields",
    description = "Updates a Todoist task via `/tasks/{id}` with new content, description, priority, or due string. At least one field is required or the task fails."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Update task content",
            code = """
                id: todoist_update_task
                namespace: company.team

                tasks:
                  - id: update_task
                    type: io.kestra.plugin.todoist.UpdateTask
                    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
                    taskId: "7498765432"
                    content: "Updated task title"
                """
        ),
        @Example(
            full = true,
            title = "Update task priority and due date",
            code = """
                id: todoist_update_task_priority
                namespace: company.team

                tasks:
                  - id: update_task_priority
                    type: io.kestra.plugin.todoist.UpdateTask
                    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
                    taskId: "7498765432"
                    priority: 4
                    dueString: "tomorrow"
                """
        )
    }
)
public class UpdateTask extends AbstractTodoistTask implements RunnableTask<UpdateTask.Output> {

    @Schema(
        title = "Task ID",
        description = "Todoist task ID to update"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> taskId;

    @Schema(
        title = "Task content",
        description = "New task title"
    )
    @PluginProperty(group = "advanced")
    private Property<String> content;

    @Schema(
        title = "Task description",
        description = "New description"
    )
    @PluginProperty(group = "advanced")
    private Property<String> taskDescription;

    @Schema(
        title = "Priority",
        description = "Priority 1 (highest) to 4 (lowest)"
    )
    @PluginProperty(group = "advanced")
    private Property<Integer> priority;

    @Schema(
        title = "Due string",
        description = "Natural-language due date (e.g., 'tomorrow', 'next Monday', '2025-12-31')"
    )
    @PluginProperty(group = "advanced")
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
    }
}

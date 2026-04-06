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
    title = "Create Todoist task",
    description = "Creates a Todoist task with required content plus optional description, priority, project, and natural-language due date. Uses Todoist API v1; fails on HTTP 4xx/5xx."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Create a simple task",
            code = """
                id: todoist_create_task
                namespace: company.team

                tasks:
                  - id: create_task
                    type: io.kestra.plugin.todoist.CreateTask
                    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
                    content: "Review pull requests"
                """
        ),
        @Example(
            full = true,
            title = "Create a task with description and priority",
            code = """
                id: todoist_create_urgent_task
                namespace: company.team

                tasks:
                  - id: create_urgent_task
                    type: io.kestra.plugin.todoist.CreateTask
                    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
                    content: "Deploy to production"
                    taskDescription: "Deploy version 2.0 after testing"
                    priority: 4
                    dueString: "tomorrow"
                """
        )
    }
)
public class CreateTask extends AbstractTodoistTask implements RunnableTask<CreateTask.Output> {

    @Schema(
        title = "Task content",
        description = "Visible task title; required"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> content;

    @Schema(
        title = "Task description",
        description = "Optional long description"
    )
    @PluginProperty(group = "advanced")
    private Property<String> taskDescription;

    @Schema(
        title = "Priority",
        description = "Priority 1 (highest) to 4 (lowest); defaults to Todoist standard when omitted"
    )
    @PluginProperty(group = "advanced")
    private Property<Integer> priority;

    @Schema(
        title = "Project ID",
        description = "Target project ID; leave null for Inbox"
    )
    @PluginProperty(group = "connection")
    private Property<String> projectId;

    @Schema(
        title = "Due string",
        description = "Natural-language due date parsed by Todoist (e.g., 'tomorrow', 'next Monday', '2025-12-31')"
    )
    @PluginProperty(group = "advanced")
    private Property<String> dueString;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String rToken = runContext.render(apiToken).as(String.class).orElseThrow();
        String rTaskContent = runContext.render(content).as(String.class).orElseThrow();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("content", rTaskContent);

        runContext.render(taskDescription).as(String.class).ifPresent(d -> requestBody.put("description", d));
        runContext.render(priority).as(Integer.class).ifPresent(p -> requestBody.put("priority", p));
        runContext.render(projectId).as(String.class).ifPresent(p -> requestBody.put("project_id", p));
        runContext.render(dueString).as(String.class).ifPresent(d -> requestBody.put("due_string", d));

        String jsonBody = JacksonMapper.ofJson().writeValueAsString(requestBody);

        HttpRequest request = createRequestBuilder(rToken, BASE_URL + "/tasks")
            .method("POST")
            .body(HttpRequest.StringRequestBody.builder().content(jsonBody).build())
            .build();

        HttpResponse<String> response = sendRequest(runContext, request);

        if (response.getStatus().getCode() >= 400) {
            throw new Exception("Failed to create task: " + response.getStatus().getCode() + " - " + response.getBody());
        }

        logger.info("Task created successfully");

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
            description = "The ID of the created task"
        )
        private final String taskId;
    }
}

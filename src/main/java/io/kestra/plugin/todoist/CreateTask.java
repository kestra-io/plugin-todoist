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
    title = "Create a new task in Todoist",
    description = "Creates a new task in Todoist with the specified content and optional parameters"
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
        description = "The content/title of the task"
    )
    @NotNull
    private Property<String> content;

    @Schema(
        title = "Task description",
        description = "A description for the task"
    )
    private Property<String> taskDescription;

    @Schema(
        title = "Priority",
        description = "Task priority from 1 (normal) to 4 (urgent)"
    )
    private Property<Integer> priority;

    @Schema(
        title = "Project ID",
        description = "The ID of the project to add the task to"
    )
    private Property<String> projectId;

    @Schema(
        title = "Due string",
        description = "Human-defined task due date (e.g., 'tomorrow', 'next Monday', '2025-12-31')"
    )
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
            .url(result.get("url").toString())
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

        @Schema(
            title = "Task URL",
            description = "The URL to view the task in Todoist"
        )
        private final String url;
    }
}

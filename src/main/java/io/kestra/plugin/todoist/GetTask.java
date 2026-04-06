package io.kestra.plugin.todoist;

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
    title = "Fetch Todoist task by ID",
    description = "Reads a Todoist task via `/tasks/{id}` and returns the full task object as a map."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Get a task by ID",
            code = """
                id: todoist_get_task
                namespace: company.team

                tasks:
                  - id: get_task
                    type: io.kestra.plugin.todoist.GetTask
                    apiToken: "{{ secret('TODOIST_API_TOKEN') }}"
                    taskId: "7498765432"
                """
        )
    }
)
public class GetTask extends AbstractTodoistTask implements RunnableTask<GetTask.Output> {

    @Schema(
        title = "Task ID",
        description = "Todoist task ID to read"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> taskId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String rToken = runContext.render(apiToken).as(String.class).orElseThrow();
        String rTaskId = runContext.render(taskId).as(String.class).orElseThrow();

        HttpRequest request = createRequestBuilder(rToken, BASE_URL + "/tasks/" + rTaskId)
            .method("GET")
            .build();

        HttpResponse<String> response = sendRequest(runContext, request);

        if (response.getStatus().getCode() >= 400) {
            throw new IllegalArgumentException("Failed to get task: " + response.getStatus().getCode() + " - " + response.getBody());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> task = JacksonMapper.ofJson().readValue(response.getBody(), Map.class);

        logger.info("Task {} retrieved successfully", rTaskId);

        return Output.builder()
            .task(task)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Task",
            description = "Complete task object returned by Todoist"
        )
        private final Map<String, Object> task;
    }
}

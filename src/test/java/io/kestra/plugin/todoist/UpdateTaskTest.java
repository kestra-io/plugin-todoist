package io.kestra.plugin.todoist;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.junit.annotations.KestraTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class UpdateTaskTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @EnabledIf(value = "isApiTokenSet", disabledReason = "TODOIST_API_TOKEN environment variable not set")
    void testUpdateTask() throws Exception {
        String apiToken = System.getenv("TODOIST_API_TOKEN");
        RunContext runContext = runContextFactory.of();

        CreateTask createTask = CreateTask.builder()
            .apiToken(Property.ofValue(apiToken))
            .content(Property.ofValue("Test task for update"))
            .build();

        CreateTask.Output createOutput = createTask.run(runContext);
        String taskId = createOutput.getTaskId();

        UpdateTask updateTask = UpdateTask.builder()
            .apiToken(Property.ofValue(apiToken))
            .taskId(Property.ofValue(taskId))
            .content(Property.ofValue("Updated test task"))
            .priority(Property.ofValue(4))
            .build();

        UpdateTask.Output updateOutput = updateTask.run(runContext);

        assertThat(updateOutput.getTaskId(), equalTo(taskId));
        assertThat(updateOutput.getUrl(), notNullValue());

        DeleteTask deleteTask = DeleteTask.builder()
            .apiToken(Property.ofValue(apiToken))
            .taskId(Property.ofValue(taskId))
            .build();

        deleteTask.run(runContext);
    }

    static boolean isApiTokenSet() {
        String token = System.getenv("TODOIST_API_TOKEN");
        return token != null && !token.isEmpty();
    }
}

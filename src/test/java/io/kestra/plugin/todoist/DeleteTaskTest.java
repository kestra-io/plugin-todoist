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
class DeleteTaskTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @EnabledIf(value = "isApiTokenSet", disabledReason = "TODOIST_API_TOKEN environment variable not set")
    void testDeleteTask() throws Exception {
        String apiToken = System.getenv("TODOIST_API_TOKEN");
        RunContext runContext = runContextFactory.of();

        CreateTask createTask = CreateTask.builder()
            .apiToken(Property.of(apiToken))
            .content(Property.of("Test task for deletion"))
            .build();

        CreateTask.Output createOutput = createTask.run(runContext);
        String taskId = createOutput.getTaskId();

        assertThat(taskId, notNullValue());

        DeleteTask deleteTask = DeleteTask.builder()
            .apiToken(Property.of(apiToken))
            .taskId(Property.of(taskId))
            .build();

        deleteTask.run(runContext);

        GetTask getTask = GetTask.builder()
            .apiToken(Property.of(apiToken))
            .taskId(Property.of(taskId))
            .build();

        try {
            getTask.run(runContext);
            throw new AssertionError("Expected task to be deleted");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Failed to get task"));
        }
    }

    static boolean isApiTokenSet() {
        String token = System.getenv("TODOIST_API_TOKEN");
        return token != null && !token.isEmpty();
    }
}

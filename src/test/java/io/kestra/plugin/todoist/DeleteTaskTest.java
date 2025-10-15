package io.kestra.plugin.todoist;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.junit.annotations.KestraTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class DeleteTaskTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testDeleteTask() throws Exception {
        String apiToken = System.getenv("TODOIST_API_TOKEN");
        
        if (apiToken == null || apiToken.isEmpty()) {
            System.out.println("Skipping test: TODOIST_API_TOKEN not set");
            return;
        }

        RunContext runContext = runContextFactory.of();

        // First create a task
        CreateTask createTask = CreateTask.builder()
            .apiToken(Property.of(apiToken))
            .content(Property.of("Test task for deletion"))
            .build();

        CreateTask.Output createOutput = createTask.run(runContext);
        String taskId = createOutput.getTaskId();

        assertThat(taskId, notNullValue());

        // Delete the task
        DeleteTask deleteTask = DeleteTask.builder()
            .apiToken(Property.of(apiToken))
            .taskId(Property.of(taskId))
            .build();

        deleteTask.run(runContext);

        // Verify deletion by trying to get the task (should fail)
        GetTask getTask = GetTask.builder()
            .apiToken(Property.of(apiToken))
            .taskId(Property.of(taskId))
            .build();

        try {
            getTask.run(runContext);
            throw new AssertionError("Expected task to be deleted");
        } catch (Exception e) {
            // Expected - task should not exist
            assertThat(e.getMessage(), containsString("Failed to get task"));
        }
    }
}

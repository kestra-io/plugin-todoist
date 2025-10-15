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
class UpdateTaskTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testUpdateTask() throws Exception {
        String apiToken = System.getenv("TODOIST_API_TOKEN");
        
        if (apiToken == null || apiToken.isEmpty()) {
            System.out.println("Skipping test: TODOIST_API_TOKEN not set");
            return;
        }

        RunContext runContext = runContextFactory.of();

        // First create a task
        CreateTask createTask = CreateTask.builder()
            .apiToken(Property.of(apiToken))
            .content(Property.of("Test task for update"))
            .build();

        CreateTask.Output createOutput = createTask.run(runContext);
        String taskId = createOutput.getTaskId();

        // Update the task
        UpdateTask updateTask = UpdateTask.builder()
            .apiToken(Property.of(apiToken))
            .taskId(Property.of(taskId))
            .content(Property.of("Updated test task"))
            .priority(Property.of(4))
            .build();

        UpdateTask.Output updateOutput = updateTask.run(runContext);

        assertThat(updateOutput.getTaskId(), equalTo(taskId));
        assertThat(updateOutput.getContent(), equalTo("Updated test task"));
        assertThat(updateOutput.getUrl(), notNullValue());

        // Clean up
        DeleteTask deleteTask = DeleteTask.builder()
            .apiToken(Property.of(apiToken))
            .taskId(Property.of(taskId))
            .build();

        deleteTask.run(runContext);
    }
}

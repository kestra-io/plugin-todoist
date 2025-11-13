package io.kestra.plugin.todoist;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.junit.annotations.KestraTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class ListTasksTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @EnabledIf(value = "isApiTokenSet", disabledReason = "TODOIST_API_TOKEN environment variable not set")
    void testListTasks() throws Exception {
        String apiToken = System.getenv("TODOIST_API_TOKEN");
        RunContext runContext = runContextFactory.of();
        List<String> createdTaskIds = new ArrayList<>();

        try {
            ListTasks listTask = ListTasks.builder()
                .apiToken(Property.ofValue(apiToken))
                .build();
            ListTasks.Output initialOutput = listTask.run(runContext);
            int initialCount = initialOutput.getSize().intValue();

            for (int i = 1; i <= 3; i++) {
                CreateTask createTask = CreateTask.builder()
                    .apiToken(Property.ofValue(apiToken))
                    .content(Property.ofValue("Test task " + i))
                    .build();
                CreateTask.Output createOutput = createTask.run(runContext);
                createdTaskIds.add(createOutput.getTaskId());
            }

            ListTasks.Output output = listTask.run(runContext);

            assertThat(output.getRows(), notNullValue());
            assertThat(output.getSize(), notNullValue());
            assertThat(output.getSize().intValue(), is(initialCount + 3));

        } finally {
            for (String taskId : createdTaskIds) {
                try {
                    DeleteTask deleteTask = DeleteTask.builder()
                        .apiToken(Property.ofValue(apiToken))
                        .taskId(Property.ofValue(taskId))
                        .build();
                    deleteTask.run(runContext);
                } catch (Exception e) {
                    System.err.println("Failed to delete test task " + taskId + ": " + e.getMessage());
                }
            }
        }
    }

    static boolean isApiTokenSet() {
        String token = System.getenv("TODOIST_API_TOKEN");
        return token != null && !token.isEmpty();
    }
}

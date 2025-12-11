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
            // Note: API v1 uses pagination (50 items per page by default)
            // We may not see all tasks if they span multiple pages
            // So we check that we got at least the initial count, and verify our created tasks are present
            assertThat(output.getSize().intValue(), greaterThanOrEqualTo(initialCount));
            
            // Verify that at least one of our created tasks is in the results
            boolean foundCreatedTask = output.getRows().stream()
                .anyMatch(task -> createdTaskIds.contains(task.get("id").toString()));
            assertThat("At least one created task should be in the results", foundCreatedTask, is(true));

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

    @Test
    @EnabledIf(value = "isApiTokenSet", disabledReason = "TODOIST_API_TOKEN environment variable not set")
    void testListTasksWithFilter() throws Exception {
        String apiToken = System.getenv("TODOIST_API_TOKEN");
        RunContext runContext = runContextFactory.of();
        List<String> createdTaskIds = new ArrayList<>();

        try {
            // Create a task with "today" due date
            CreateTask createTask = CreateTask.builder()
                .apiToken(Property.ofValue(apiToken))
                .content(Property.ofValue("Test task for filter"))
                .dueString(Property.ofValue("today"))
                .build();
            CreateTask.Output createOutput = createTask.run(runContext);
            createdTaskIds.add(createOutput.getTaskId());

            // List tasks with filter
            ListTasks listTask = ListTasks.builder()
                .apiToken(Property.ofValue(apiToken))
                .filter(Property.ofValue("today"))
                .build();
            ListTasks.Output output = listTask.run(runContext);

            assertThat(output.getRows(), notNullValue());
            assertThat(output.getSize(), notNullValue());
            assertThat(output.getSize().intValue(), greaterThanOrEqualTo(1));

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

    @Test
    @EnabledIf(value = "isApiTokenSet", disabledReason = "TODOIST_API_TOKEN environment variable not set")
    void testListTasksWithFilterAndProjectIdThrowsException() throws Exception {
        String apiToken = System.getenv("TODOIST_API_TOKEN");
        RunContext runContext = runContextFactory.of();

        ListTasks listTask = ListTasks.builder()
            .apiToken(Property.ofValue(apiToken))
            .filter(Property.ofValue("today"))
            .projectId(Property.ofValue("123456"))
            .build();

        try {
            listTask.run(runContext);
            // If we get here, the test should fail
            assertThat("Expected IllegalArgumentException to be thrown", false);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Cannot use both 'filter' and 'projectId'"));
        }
    }

    static boolean isApiTokenSet() {
        String token = System.getenv("TODOIST_API_TOKEN");
        return token != null && !token.isEmpty();
    }
}

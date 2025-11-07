package io.kestra.plugin.todoist;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.junit.annotations.KestraTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

@KestraTest
class CompleteTaskTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @EnabledIf(value = "isApiTokenSet", disabledReason = "TODOIST_API_TOKEN environment variable not set")
    void testCompleteTask() throws Exception {
        String apiToken = System.getenv("TODOIST_API_TOKEN");
        RunContext runContext = runContextFactory.of();

        CreateTask createTask = CreateTask.builder()
            .apiToken(Property.of(apiToken))
            .content(Property.of("Test task for CompleteTask"))
            .build();

        CreateTask.Output createOutput = createTask.run(runContext);

        CompleteTask completeTask = CompleteTask.builder()
            .apiToken(Property.of(apiToken))
            .taskId(Property.of(createOutput.getTaskId()))
            .build();

        completeTask.run(runContext);
    }

    static boolean isApiTokenSet() {
        String token = System.getenv("TODOIST_API_TOKEN");
        return token != null && !token.isEmpty();
    }
}

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
class CreateTaskTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @EnabledIf(value = "isApiTokenSet", disabledReason = "TODOIST_API_TOKEN environment variable not set")
    void testCreateTask() throws Exception {
        String apiToken = System.getenv("TODOIST_API_TOKEN");
        RunContext runContext = runContextFactory.of();

        CreateTask task = CreateTask.builder()
            .apiToken(Property.ofValue(apiToken))
            .content(Property.ofValue("Test task from Kestra"))
            .taskDescription(Property.ofValue("This is a test task created by the Kestra Todoist plugin"))
            .priority(Property.ofValue(1))
            .build();

        CreateTask.Output output = task.run(runContext);

        assertThat(output.getTaskId(), notNullValue());
    }

    static boolean isApiTokenSet() {
        String token = System.getenv("TODOIST_API_TOKEN");
        return token != null && !token.isEmpty();
    }
}

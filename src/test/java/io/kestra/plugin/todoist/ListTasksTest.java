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
class ListTasksTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testListTasks() throws Exception {
        String apiToken = System.getenv("TODOIST_API_TOKEN");
        
        if (apiToken == null || apiToken.isEmpty()) {
            System.out.println("Skipping test: TODOIST_API_TOKEN not set");
            return;
        }

        RunContext runContext = runContextFactory.of();

        ListTasks task = ListTasks.builder()
            .apiToken(Property.of(apiToken))
            .build();

        ListTasks.Output output = task.run(runContext);

        assertThat(output.getTasks(), notNullValue());
        assertThat(output.getCount(), notNullValue());
        assertThat(output.getCount(), greaterThanOrEqualTo(0));
    }
}

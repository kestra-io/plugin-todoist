package io.kestra.plugin.todoist;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import okhttp3.*;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a new task in Todoist",
    description = "Creates a new task in Todoist with the specified content and optional parameters"
)
@Plugin(
    examples = {
        @Example(
            title = "Create a simple task",
            code = {
                "apiToken: \"{{ secret('TODOIST_API_TOKEN') }}\"",
                "content: \"Review pull requests\""
            }
        ),
        @Example(
            title = "Create a task with description and priority",
            code = {
                "apiToken: \"{{ secret('TODOIST_API_TOKEN') }}\"",
                "content: \"Deploy to production\"",
                "taskDescription: \"Deploy version 2.0 after testing\"",
                "priority: 4"
            }
        )
    }
)
public class CreateTask extends AbstractTodoistTask implements RunnableTask<CreateTask.Output> {
    
    @Schema(
        title = "Task content",
        description = "The content/title of the task"
    )
    private Property<String> content;
    
    @Schema(
        title = "Task description",
        description = "A description for the task"
    )
    private Property<String> taskDescription;
    
    @Schema(
        title = "Priority",
        description = "Task priority from 1 (normal) to 4 (urgent)"
    )
    private Property<Integer> priority;
    
    @Schema(
        title = "Project ID",
        description = "The ID of the project to add the task to"
    )
    private Property<String> projectId;
    
    @Schema(
        title = "Due string",
        description = "Human-defined task due date (e.g., 'tomorrow', 'next Monday', '2025-12-31')"
    )
    private Property<String> dueString;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String token = runContext.render(apiToken).as(String.class).orElseThrow();
        String taskContent = runContext.render(content).as(String.class).orElseThrow();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("content", taskContent);
        
        runContext.render(taskDescription).as(String.class).ifPresent(d -> requestBody.put("description", d));
        runContext.render(priority).as(Integer.class).ifPresent(p -> requestBody.put("priority", p));
        runContext.render(projectId).as(String.class).ifPresent(p -> requestBody.put("project_id", p));
        runContext.render(dueString).as(String.class).ifPresent(d -> requestBody.put("due_string", d));
        
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(requestBody);
        
        OkHttpClient client = createHttpClient();
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        
        Request request = createRequestBuilder(token)
            .url(BASE_URL + "/tasks")
            .post(body)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                throw new Exception("Failed to create task: " + response.code() + " - " + responseBody);
            }
            
            logger.info("Task created successfully");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(responseBody, Map.class);
            
            return Output.builder()
                .taskId(result.get("id").toString())
                .content(result.get("content").toString())
                .url(result.get("url").toString())
                .response(result)
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Task ID",
            description = "The ID of the created task"
        )
        private final String taskId;
        
        @Schema(
            title = "Task content",
            description = "The content of the created task"
        )
        private final String content;
        
        @Schema(
            title = "Task URL",
            description = "The URL to view the task in Todoist"
        )
        private final String url;
        
        @Schema(
            title = "Full response",
            description = "The complete response from Todoist API"
        )
        private final Map<String, Object> response;
    }
}

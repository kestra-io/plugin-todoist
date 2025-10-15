package io.kestra.plugin.todoist;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractTodoistTask extends Task {
    
    @Schema(
        title = "Todoist API token",
        description = "Your Todoist API token for authentication. Get it from https://todoist.com/app/settings/integrations/developer"
    )
    @NotNull
    protected Property<String> apiToken;

    protected static final String BASE_URL = "https://api.todoist.com/rest/v2";
    
    protected HttpRequest.HttpRequestBuilder createRequestBuilder(String token, String url) {
        return HttpRequest.builder()
            .uri(URI.create(url))
            .addHeader("Authorization", "Bearer " + token)
            .addHeader("Content-Type", "application/json");
    }
    
    protected HttpResponse<String> sendRequest(RunContext runContext, HttpRequest request) throws Exception {
        HttpClient client = HttpClient.builder()
            .runContext(runContext)
            .build();
        return client.request(request, String.class);
    }
}

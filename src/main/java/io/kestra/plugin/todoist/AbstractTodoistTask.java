package io.kestra.plugin.todoist;

import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;

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
    protected Property<String> apiToken;

    protected static final String BASE_URL = "https://api.todoist.com/rest/v2";
    
    protected OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder().build();
    }
    
    protected Request.Builder createRequestBuilder(String apiToken) {
        return new Request.Builder()
            .addHeader("Authorization", "Bearer " + apiToken)
            .addHeader("Content-Type", "application/json");
    }
}

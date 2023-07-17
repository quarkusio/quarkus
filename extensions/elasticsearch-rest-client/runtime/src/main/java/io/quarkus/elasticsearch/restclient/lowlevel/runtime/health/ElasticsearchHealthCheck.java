package io.quarkus.elasticsearch.restclient.lowlevel.runtime.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import io.vertx.core.json.JsonObject;

@Readiness
@ApplicationScoped
public class ElasticsearchHealthCheck implements HealthCheck {
    @Inject
    RestClient restClient;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Elasticsearch cluster health check").up();
        try {
            Request request = new Request("GET", "/_cluster/health");
            Response response = restClient.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = new JsonObject(responseBody);
            String status = json.getString("status");
            if ("red".equals(status)) {
                builder.down().withData("status", status);
            } else {
                builder.up().withData("status", status);
            }

        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
        return builder.build();
    }
}

package io.quarkus.elasticsearch.restclient.lowlevel.runtime.health;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import io.vertx.core.json.JsonObject;

public class ElasticsearchHealthCheckCondition {
    private final String clientName;
    private final RestClient restClient;

    public ElasticsearchHealthCheckCondition(String clientName, RestClient restClient) {
        this.clientName = clientName;
        this.restClient = restClient;
    }

    public Status check() {
        try {
            Request request = new Request("GET", "/_cluster/health");
            Response response = restClient.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = new JsonObject(responseBody);
            String status = json.getString("status");
            return new Status(clientName, status, null);
        } catch (Exception e) {
            return new Status(clientName, null, e.getMessage());
        }
    }

    public record Status(String clientName, String status, String reason) {
    }
}

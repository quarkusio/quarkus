package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Path("/fruits")
public class TestResource {
    @Inject
    RestClient restClient;

    @POST
    public void index(Fruit fruit) throws IOException {
        Request request = new Request(
                "PUT",
                "/fruits/_doc/" + fruit.id + "?refresh=true");
        request.setJsonEntity(JsonObject.mapFrom(fruit).toString());
        restClient.performRequest(request);
    }

    @GET
    @Path("/search")
    public List<Fruit> search(@QueryParam("term") String term, @QueryParam("match") String match) throws IOException {
        Request request = new Request(
                "GET",
                "/fruits/_search");
        //construct a JSON query like {"query": {"match": {"<term>": "<match"}}
        JsonObject termJson = new JsonObject().put(term, match);
        JsonObject matchJson = new JsonObject().put("match", termJson);
        JsonObject queryJson = new JsonObject().put("query", matchJson);
        request.setJsonEntity(queryJson.encode());
        Response response = restClient.performRequest(request);
        String responseBody = EntityUtils.toString(response.getEntity());

        JsonObject json = new JsonObject(responseBody);
        JsonArray hits = json.getJsonObject("hits").getJsonArray("hits");
        List<Fruit> results = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            JsonObject hit = hits.getJsonObject(i);
            Fruit fruit = hit.getJsonObject("_source").mapTo(Fruit.class);
            results.add(fruit);
        }
        return results;
    }

    public static class Fruit {
        public String id;
        public String name;
        public String color;
    }
}

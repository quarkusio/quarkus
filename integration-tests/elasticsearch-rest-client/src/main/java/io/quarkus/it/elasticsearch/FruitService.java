package io.quarkus.it.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class FruitService {
    @Inject
    RestClient restClient;

    public void index(Fruit fruit) throws IOException {
        Request request = new Request(
                "PUT",
                "/fruits/_doc/" + fruit.id);
        request.setJsonEntity(JsonObject.mapFrom(fruit).toString());
        restClient.performRequest(request);
    }

    public Fruit get(String id) throws IOException {
        Request request = new Request(
                "GET",
                "/fruits/_doc/" + id);
        Response response = restClient.performRequest(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        JsonObject json = new JsonObject(responseBody);
        return json.getJsonObject("_source").mapTo(Fruit.class);
    }

    public List<Fruit> searchByColor(String color) throws IOException {
        return search("color", color);
    }

    public List<Fruit> searchByName(String name) throws IOException {
        return search("name", name);
    }

    private List<Fruit> search(String term, String match) throws IOException {
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

    public void index(List<Fruit> list) throws IOException {

        var entityList = new ArrayList<JsonObject>();

        for (var fruit : list) {

            entityList.add(new JsonObject().put("index", new JsonObject()
                    .put("_index", "fruits").put("_id", fruit.id)));
            entityList.add(JsonObject.mapFrom(fruit));
        }

        Request request = new Request(
                "POST", "fruits/_bulk?pretty");
        request.setEntity(new StringEntity(
                toNdJsonString(entityList),
                ContentType.create("application/x-ndjson")));
        restClient.performRequest(request);
    }

    public void delete(List<String> identityList) throws IOException {

        var entityList = new ArrayList<JsonObject>();

        for (var id : identityList) {
            entityList.add(new JsonObject().put("delete",
                    new JsonObject().put("_index", "fruits").put("_id", id)));
        }

        Request request = new Request(
                "POST", "fruits/_bulk?pretty");
        request.setEntity(new StringEntity(
                toNdJsonString(entityList),
                ContentType.create("application/x-ndjson")));
        restClient.performRequest(request);
    }

    private static String toNdJsonString(List<JsonObject> objects) {
        return objects.stream()
                .map(JsonObject::encode)
                .collect(Collectors.joining("\n", "", "\n"));
    }
}

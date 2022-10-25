package io.quarkus.it.elasticsearch.highlevel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class FruitService {
    @Inject
    RestHighLevelClient restHighLevelClient;

    public void index(Fruit fruit) throws IOException {
        IndexRequest request = new IndexRequest("fruits");
        request.id(fruit.id);
        request.source(JsonObject.mapFrom(fruit).toString(), XContentType.JSON);
        restHighLevelClient.index(request, RequestOptions.DEFAULT);
    }

    public Fruit get(String id) throws IOException {
        GetRequest getRequest = new GetRequest("fruits", id);
        GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        if (getResponse.isExists()) {
            String sourceAsString = getResponse.getSourceAsString();
            JsonObject json = new JsonObject(sourceAsString);
            return json.mapTo(Fruit.class);
        }
        return null;
    }

    public List<Fruit> searchByColor(String color) throws IOException {
        return search("color", color);
    }

    public List<Fruit> searchByName(String name) throws IOException {
        return search("name", name);
    }

    private List<Fruit> search(String term, String match) throws IOException {
        SearchRequest searchRequest = new SearchRequest("fruits");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery(term, match));
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        List<Fruit> results = new ArrayList<>(hits.getHits().length);
        for (SearchHit hit : hits.getHits()) {
            String sourceAsString = hit.getSourceAsString();
            JsonObject json = new JsonObject(sourceAsString);
            results.add(json.mapTo(Fruit.class));
        }
        return results;
    }
}

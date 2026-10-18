package io.quarkus.it.elasticsearch.java;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;

@ApplicationScoped
public class FruitService {
    @Inject
    ElasticsearchClient client;

    public void index(Fruit fruit) throws IOException {
        IndexRequest<Fruit> request = IndexRequest.of(
                b -> b.index("fruits")
                        .id(fruit.id)
                        .document(fruit));
        client.index(request);
    }

    public Fruit get(String id) throws IOException {
        GetRequest getRequest = GetRequest.of(
                b -> b.index("fruits")
                        .id(id));
        GetResponse<Fruit> getResponse = client.get(getRequest, Fruit.class);
        if (getResponse.found()) {
            return getResponse.source();
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
        SearchRequest searchRequest = SearchRequest.of(
                b -> b.index("fruits")
                        .query(QueryBuilders.match().field(term).query(FieldValue.of(match)).build()._toQuery()));

        SearchResponse<Fruit> searchResponse = client.search(searchRequest, Fruit.class);
        HitsMetadata<Fruit> hits = searchResponse.hits();
        return hits.hits().stream().map(Hit::source).collect(Collectors.toList());
    }

    public List<Fruit> searchWithJson(String json) throws IOException {
        SearchRequest searchRequest;
        try (var jsonReader = new StringReader(json)) {
            searchRequest = SearchRequest.of(b -> b.index("fruits").withJson(jsonReader));
        }
        SearchResponse<Fruit> searchResponse = client.search(searchRequest, Fruit.class);
        HitsMetadata<Fruit> hits = searchResponse.hits();
        return hits.hits().stream().map(Hit::source).collect(Collectors.toList());
    }

    public void index(List<Fruit> list) throws IOException {

        BulkRequest.Builder br = new BulkRequest.Builder();

        for (var fruit : list) {
            br.operations(op -> op
                    .index(idx -> idx.index("fruits").id(fruit.id).document(fruit)));
        }

        BulkResponse result = client.bulk(br.build());

        if (result.errors()) {
            throw new RuntimeException("The indexing operation encountered errors.");
        }
    }

    public void delete(List<String> list) throws IOException {

        BulkRequest.Builder br = new BulkRequest.Builder();

        for (var id : list) {
            br.operations(op -> op.delete(idx -> idx.index("fruits").id(id)));
        }

        BulkResponse result = client.bulk(br.build());

        if (result.errors()) {
            throw new RuntimeException("The indexing operation encountered errors.");
        }
    }
}

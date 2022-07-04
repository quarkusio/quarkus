package io.quarkus.redis.runtime.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.vertx.mutiny.redis.client.Response;

public class TransactionHolder {

    private List<Function<Response, Object>> mappers = new ArrayList<>();
    private volatile boolean discarded = false;

    public void enqueue(Function<Response, Object> mapper) {
        mappers.add(mapper);
    }

    public List<Object> map(Response response) {
        List<Object> results = new ArrayList<>();
        for (int i = 0; i < mappers.size(); i++) {
            results.add(mappers.get(i).apply(response.get(i)));
        }
        return results;
    }

    public void discard() {
        discarded = true;
    }

    public boolean discarded() {
        return discarded;
    }
}

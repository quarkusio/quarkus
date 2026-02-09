package io.quarkus.redis.runtime.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;

public class TransactionHolder {

    private final List<Function<Response, Object>> mappers = new ArrayList<>();
    private volatile boolean discarded = false;

    public void enqueue(Function<Response, Object> mapper) {
        mappers.add(mapper);
    }

    public TransactionResult toResult(Response response) {
        boolean hasErrors = false;
        List<Object> results = new ArrayList<>();
        for (int i = 0; i < mappers.size(); i++) {
            Response responsePart = response.get(i);
            if (responsePart == null || responsePart.type() != ResponseType.ERROR) {
                // `null` is a valid result
                results.add(mappers.get(i).apply(responsePart));
            } else {
                hasErrors = true;
                results.add(responsePart.getDelegate()); // it's also an exception
            }
        }
        return new TransactionResultImpl(discarded, hasErrors, results);
    }

    public <I> OptimisticLockingTransactionResult<I> toOptimisticLockingResult(I input, Response response) {
        boolean hasErrors = false;
        List<Object> results = new ArrayList<>();
        for (int i = 0; i < mappers.size(); i++) {
            Response responsePart = response.get(i);
            if (responsePart == null || responsePart.type() != ResponseType.ERROR) {
                // `null` is a valid result
                results.add(mappers.get(i).apply(responsePart));
            } else {
                hasErrors = true;
                results.add(responsePart.getDelegate()); // it's also an exception
            }
        }
        return new OptimisticLockingTransactionResultImpl<>(discarded, hasErrors, input, results);
    }

    public void discard() {
        discarded = true;
    }

    public boolean discarded() {
        return discarded;
    }
}

package io.quarkus.redis.runtime.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.quarkus.redis.datasource.transactions.TransactionResult;

public class TransactionResultImpl implements TransactionResult {

    public static final TransactionResult DISCARDED = new TransactionResultImpl(true, Collections.emptyList());

    private final List<Object> results = new ArrayList<>();
    private final boolean discarded;

    public TransactionResultImpl(boolean discarded, List<Object> res) {
        this.results.addAll(res);
        this.discarded = discarded;
    }

    @Override
    public boolean discarded() {
        return discarded;
    }

    @Override
    public int size() {
        return results.size();
    }

    @Override
    public boolean isEmpty() {
        return results.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(int index) {
        return (T) results.get(index);
    }

    @Override
    public Iterator<Object> iterator() {
        return results.iterator();
    }
}

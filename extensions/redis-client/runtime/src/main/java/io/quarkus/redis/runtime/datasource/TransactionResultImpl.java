package io.quarkus.redis.runtime.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.quarkus.redis.datasource.transactions.TransactionResult;

public class TransactionResultImpl implements TransactionResult {

    public static final TransactionResult DISCARDED = new TransactionResultImpl(true, false, Collections.emptyList());

    private final List<Object> results = new ArrayList<>();
    private final boolean discarded;
    private final boolean hasErrors;

    public TransactionResultImpl(boolean discarded, boolean hasErrors, List<Object> res) {
        this.results.addAll(res);
        this.discarded = discarded;
        this.hasErrors = hasErrors;
    }

    @Override
    public boolean discarded() {
        return discarded;
    }

    @Override
    public boolean hasErrors() {
        return hasErrors;
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

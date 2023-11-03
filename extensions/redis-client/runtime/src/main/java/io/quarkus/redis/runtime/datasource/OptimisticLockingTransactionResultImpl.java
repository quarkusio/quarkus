package io.quarkus.redis.runtime.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;

public class OptimisticLockingTransactionResultImpl<I> implements OptimisticLockingTransactionResult<I> {

    private final List<Object> results = new ArrayList<>();
    private final boolean discarded;
    private final I input;

    public OptimisticLockingTransactionResultImpl(boolean discarded, I input, List<Object> res) {
        this.results.addAll(res);
        this.discarded = discarded;
        this.input = input;
    }

    public static <I> OptimisticLockingTransactionResult<I> discarded(I input) {
        return new OptimisticLockingTransactionResultImpl<>(true, input, Collections.emptyList());
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

    @Override
    public I getPreTransactionResult() {
        return input;
    }
}

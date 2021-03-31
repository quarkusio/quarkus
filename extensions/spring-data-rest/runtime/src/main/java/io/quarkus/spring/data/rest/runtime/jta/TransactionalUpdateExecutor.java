package io.quarkus.spring.data.rest.runtime.jta;

import java.util.function.Supplier;

import javax.inject.Singleton;
import javax.transaction.Transactional;

import io.quarkus.rest.data.panache.runtime.UpdateExecutor;

@Singleton
public class TransactionalUpdateExecutor implements UpdateExecutor {

    @Override
    @Transactional
    public <T> T execute(Supplier<T> supplier) {
        return supplier.get();
    }
}

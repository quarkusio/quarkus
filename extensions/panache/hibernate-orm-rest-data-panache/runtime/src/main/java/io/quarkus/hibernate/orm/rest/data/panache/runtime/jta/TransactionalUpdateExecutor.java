package io.quarkus.hibernate.orm.rest.data.panache.runtime.jta;

import java.util.function.Supplier;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import io.quarkus.rest.data.panache.runtime.UpdateExecutor;

@Singleton
public class TransactionalUpdateExecutor implements UpdateExecutor {

    @Override
    @Transactional
    public <T> T execute(Supplier<T> supplier) {
        return supplier.get();
    }
}

package io.quarkus.rest.data.panache.runtime.jta;

import java.util.function.Supplier;

import javax.inject.Singleton;
import javax.transaction.Transactional;

@Singleton
public class TransactionalExecutor {

    @Transactional
    public <T> T execute(Supplier<T> supplier) {
        return supplier.get();
    }
}

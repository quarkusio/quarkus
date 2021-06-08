package io.quarkus.undertow.websockets.client.runtime;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class ExecutorSupplier implements Supplier<Executor> {

    static volatile Executor executor;

    @Override
    public Executor get() {
        return executor;
    }
}

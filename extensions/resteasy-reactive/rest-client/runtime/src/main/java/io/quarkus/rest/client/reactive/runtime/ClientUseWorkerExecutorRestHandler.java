package io.quarkus.rest.client.reactive.runtime;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

import io.quarkus.runtime.ExecutorRecorder;
import io.vertx.core.Context;

/**
 * This is added by the Reactive Rest Client if the `@Blocking` annotation is used in some scenarios. For example, when users
 * provide a custom ResponseExceptionMapper that is annotates with the `@Blocking` annotation.
 *
 * Then this handler is applied, the execution of the next handlers will use the worker thread pool.
 */
public class ClientUseWorkerExecutorRestHandler implements ClientRestHandler {

    private volatile Executor executor;
    private final Supplier<Executor> supplier = new Supplier<Executor>() {
        @Override
        public Executor get() {
            return ExecutorRecorder.getCurrent();
        }
    };

    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        if (!Context.isOnEventLoopThread()) {
            return; //already dispatched
        }

        if (executor == null) {
            executor = supplier.get();
        }

        requestContext.suspend();
        requestContext.resume(executor);
    }
}

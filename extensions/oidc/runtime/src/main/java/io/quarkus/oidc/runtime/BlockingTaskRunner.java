package io.quarkus.oidc.runtime;

import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.smallrye.mutiny.Uni;

public class BlockingTaskRunner<T> implements OidcRequestContext<T> {

    private final BlockingSecurityExecutor blockingExecutor;

    public BlockingTaskRunner() {
        this.blockingExecutor = new BlockingSecurityExecutor() {

            private volatile BlockingSecurityExecutor delegate = null;

            @Override
            public <O> Uni<O> executeBlocking(Supplier<? extends O> supplier) {
                if (delegate == null) {
                    delegate = Arc.container().select(BlockingSecurityExecutor.class).get();
                }
                return delegate.executeBlocking(supplier);
            }
        };
    }

    public BlockingTaskRunner(BlockingSecurityExecutor blockingExecutor) {
        this.blockingExecutor = blockingExecutor;
    }

    public Uni<T> runBlocking(Supplier<T> function) {
        return blockingExecutor.executeBlocking(function);
    }
}
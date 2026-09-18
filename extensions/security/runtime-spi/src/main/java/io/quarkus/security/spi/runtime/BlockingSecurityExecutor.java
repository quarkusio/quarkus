package io.quarkus.security.spi.runtime;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.smallrye.mutiny.Uni;

/**
 * Blocking executor used for security purposes such {@link AuthenticationRequestContext#runBlocking(Supplier)}.
 * Extensions may provide their own implementation if they need a single thread pool.
 */
public interface BlockingSecurityExecutor {

    <T> Uni<T> executeBlocking(Supplier<? extends T> supplier);

    static BlockingSecurityExecutor createBlockingExecutor(Supplier<Executor> executorSupplier) {
        return new BlockingSecurityExecutor() {
            @Override
            public <T> Uni<T> executeBlocking(Supplier<? extends T> function) {
                return Uni.createFrom().deferred(new Supplier<Uni<? extends T>>() {
                    @Override
                    public Uni<? extends T> get() {
                        Uni<T> uni = Uni.createFrom().item(function);
                        if (BlockingOperationControl.isBlockingAllowed()) {
                            return uni;
                        }
                        return uni.runSubscriptionOn(executorSupplier.get());
                    }
                });
            }
        };
    }

}

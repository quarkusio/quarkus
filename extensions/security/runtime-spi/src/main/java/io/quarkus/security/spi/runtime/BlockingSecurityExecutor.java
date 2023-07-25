package io.quarkus.security.spi.runtime;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

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
                return Uni.createFrom().emitter(new Consumer<UniEmitter<? super T>>() {
                    @Override
                    public void accept(UniEmitter<? super T> uniEmitter) {
                        executorSupplier.get().execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    uniEmitter.complete(function.get());
                                } catch (Throwable t) {
                                    uniEmitter.fail(t);
                                }
                            }
                        });
                    }
                });
            }
        };
    }

}

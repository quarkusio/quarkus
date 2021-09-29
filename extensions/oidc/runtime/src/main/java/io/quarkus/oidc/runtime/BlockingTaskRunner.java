package io.quarkus.oidc.runtime;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ExecutorRecorder;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

public class BlockingTaskRunner<T> implements OidcRequestContext<T> {
    public Uni<T> runBlocking(Supplier<T> function) {
        return Uni.createFrom().deferred(new Supplier<Uni<? extends T>>() {
            @Override
            public Uni<T> get() {
                if (BlockingOperationControl.isBlockingAllowed()) {
                    try {
                        return Uni.createFrom().item(function.get());
                    } catch (Throwable t) {
                        return Uni.createFrom().failure(t);
                    }
                } else {
                    return Uni.createFrom().emitter(new Consumer<UniEmitter<? super T>>() {
                        @Override
                        public void accept(UniEmitter<? super T> uniEmitter) {
                            ExecutorRecorder.getCurrent().execute(new Runnable() {
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
            }
        });
    }
}
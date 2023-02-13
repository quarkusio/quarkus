package io.quarkus.hibernate.reactive.runtime;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.hibernate.reactive.common.spi.MutinyImplementor;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;

public class ReactiveSessionProducer {

    @Inject
    MutinyImplementor mutinyImplementor;

    @Produces
    @RequestScoped
    @DefaultBean
    @Unremovable
    public Mutiny.Session createMutinySession() {
        return mutinyImplementor.newSession();
    }

    public void disposeMutinySession(@Disposes Mutiny.Session reactiveSession) {
        if (reactiveSession != null) {
            //N.B. make sure to subscribe as this is a Mutiny based Session:
            // operations don't happen at all if there is no subscription.
            final CompletableFuture<Void> closeOperation = reactiveSession.close()
                    .subscribe()
                    .asCompletionStage();
            if (!io.vertx.core.Context.isOnVertxThread()) {
                //When invoked from blocking code, behave as expected and block on the operation
                //so to not starve resources with a deferred close.
                closeOperation.join();
            }
            // [else] no need to block. Worst that can happen is that the opening
            // of a new Mutiny.Session needs to wait for an available connection,
            // which implicitly orders it as "downstream" from the previous close
            // to have actually happened as the connection pool is reactive.
            // Also, if connections are available there is no real need to wait for
            // it, so this should be good.
        }
    }

}

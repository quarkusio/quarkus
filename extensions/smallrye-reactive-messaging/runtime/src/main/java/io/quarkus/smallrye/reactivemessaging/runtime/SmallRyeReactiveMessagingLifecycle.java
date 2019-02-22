package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.extension.MediatorManager;

@Dependent
public class SmallRyeReactiveMessagingLifecycle {

    @Inject
    MediatorManager mediatorManager;

    void onApplicationStart(@Observes StartupEvent event) {
        CompletableFuture<Void> future = mediatorManager.initializeAndRun();
        try {
            future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}

package io.quarkus.security.webauthn;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.ext.auth.webauthn.Authenticator;
import io.vertx.mutiny.core.Vertx;

/**
 * WebAuthn authenticator storage which delegates to @{link WebAuthnUserProvider}.
 */
@ApplicationScoped
public class WebAuthnAuthenticatorStorage {

    @Inject
    WebAuthnUserProvider userProvider;

    @Inject
    Vertx vertx;

    public Future<List<Authenticator>> fetcher(Authenticator query) {
        Uni<List<Authenticator>> res;
        if (query.getUserName() != null)
            res = runPotentiallyBlocking(() -> userProvider.findWebAuthnCredentialsByUserName(query.getUserName()));
        else if (query.getCredID() != null)
            res = runPotentiallyBlocking(() -> userProvider.findWebAuthnCredentialsByCredID(query.getCredID()));
        else
            return Future.succeededFuture(Collections.emptyList());
        return Future.fromCompletionStage(res.subscribeAsCompletionStage());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> Uni<T> runPotentiallyBlocking(Supplier<Uni<? extends T>> supplier) {
        if (BlockingOperationControl.isBlockingAllowed()
                || isNonBlocking(userProvider.getClass())) {
            return (Uni<T>) supplier.get();
        }
        if (isRunOnVirtualThread(userProvider.getClass())) {
            return Uni.createFrom().deferred(supplier).runSubscriptionOn(VirtualThreadsRecorder.getCurrent());
        }
        // run it in a worker thread
        return vertx.executeBlocking(Uni.createFrom().deferred((Supplier) supplier));
    }

    private boolean isNonBlocking(Class<?> klass) {
        do {
            if (klass.isAnnotationPresent(NonBlocking.class))
                return true;
            if (klass.isAnnotationPresent(Blocking.class))
                return false;
            if (klass.isAnnotationPresent(RunOnVirtualThread.class))
                return false;
            klass = klass.getSuperclass();
        } while (klass != null);
        // no information, assumed non-blocking
        return true;
    }

    private boolean isRunOnVirtualThread(Class<?> klass) {
        do {
            if (klass.isAnnotationPresent(RunOnVirtualThread.class))
                return true;
            if (klass.isAnnotationPresent(Blocking.class))
                return false;
            if (klass.isAnnotationPresent(NonBlocking.class))
                return false;
            klass = klass.getSuperclass();
        } while (klass != null);
        // no information, assumed non-blocking
        return false;
    }

    public Future<Void> updater(Authenticator authenticator) {
        return Future
                .fromCompletionStage(runPotentiallyBlocking(() -> userProvider.updateOrStoreWebAuthnCredentials(authenticator))
                        .subscribeAsCompletionStage());
    }
}

package io.quarkus.smallrye.health.runtime;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import io.quarkus.arc.Arc;
import io.quarkus.vertx.core.runtime.VertxMDC;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.vertx.runtime.VertxCurrentContextFactory;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.health.AsyncHealthCheckFactory;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.vertx.MutinyHelper;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;

/**
 * Quarkus specific health check factory that runs blocking and reactive
 * health checks with different executors provided by {@link MutinyHelper}.
 */
@Singleton
public class QuarkusAsyncHealthCheckFactory extends AsyncHealthCheckFactory {

    private final Vertx vertx;

    public QuarkusAsyncHealthCheckFactory(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Uni<HealthCheckResponse> callSync(HealthCheck healthCheck) {
        Uni<HealthCheckResponse> healthCheckResponseUni = super.callSync(healthCheck);
        Context duplicatedContext = newIsolatedContext(vertx.getOrCreateContext());
        // Subscription to healthCheckResponseUni is deferred until the outer Uni is subscribed to
        return Uni.createFrom().emitter(new Consumer<>() {
            @Override
            public void accept(final UniEmitter<? super HealthCheckResponse> emitter) {
                healthCheckResponseUni
                        .runSubscriptionOn(new Executor() {
                            @Override
                            public void execute(Runnable command) {
                                duplicatedContext.executeBlocking(new Callable<Void>() {
                                    @Override
                                    public Void call() {
                                        command.run();
                                        return null;
                                    }
                                }, false).onFailure(new Handler<>() {
                                    @Override
                                    public void handle(Throwable t) {
                                        emitter.complete(HealthCheckResponse.named(healthCheck.getClass().getName())
                                                .down()
                                                .withData("error", t.getClass().getName() + ": " + t.getMessage())
                                                .build());
                                    }
                                });
                            }
                        })
                        .subscribe().with(new Consumer<>() {
                            @Override
                            public void accept(HealthCheckResponse response) {
                                emitter.complete(response);
                            }
                        }, new Consumer<>() {
                            @Override
                            public void accept(Throwable t) {
                                emitter.fail(t);
                            }
                        });
            }
        });
    }

    /**
     * Each blocking check runs on its own duplicated context, so that concurrent checks cannot clobber each other's
     * context locals (for example the MDC). When the checks are triggered by an HTTP request, the new context gets a
     * copy of the locals of the request context, so that the checks can read what the request filters stored there.
     * This mirrors what {@code VertxCoreRecorder#executionContextHandler} does for tasks dispatched to other threads.
     */
    private static Context newIsolatedContext(Context current) {
        if (!VertxContext.isDuplicatedContext(current)) {
            return VertxContext.createNewDuplicatedContext(current);
        }
        ContextInternal copy = ((ContextInternal) current).duplicate();
        VertxContext.localContextData(copy).putAll(VertxContext.localContextData(current));
        VertxMDC.MDC_LOCAL.get(copy, ConcurrentHashMap::new).putAll(VertxMDC.MDC_LOCAL.get(current, ConcurrentHashMap::new));
        if (Arc.container().getCurrentContextFactory() instanceof VertxCurrentContextFactory currentContextFactory) {
            currentContextFactory.keys().forEach(VertxContext.localContextData(copy)::remove);
        }
        VertxContextSafetyToggle.setContextSafe(copy, true);
        return copy;
    }

    @Override
    public Uni<HealthCheckResponse> callAsync(AsyncHealthCheck asyncHealthCheck) {
        Uni<HealthCheckResponse> healthCheckResponseUni = super.callAsync(asyncHealthCheck);
        return healthCheckResponseUni.runSubscriptionOn(MutinyHelper.executor(vertx));
    }
}

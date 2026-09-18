package io.quarkus.smallrye.health.runtime;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Function;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import io.smallrye.common.vertx.VertxContext;
import io.smallrye.health.AsyncHealthCheckFactory;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.MutinyHelper;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

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
        Context duplicatedContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                duplicatedContext.executeBlocking(new Callable<Void>() {
                    @Override
                    public Void call() {
                        command.run();
                        return null;
                    }
                }, false);
            }
        };
        return healthCheckResponseUni
                .runSubscriptionOn(executor)
                .onFailure().recoverWithItem(new Function<Throwable, HealthCheckResponse>() {
                    @Override
                    public HealthCheckResponse apply(Throwable t) {
                        return HealthCheckResponse.named(healthCheck.getClass().getName())
                                .down()
                                .withData("error", t.getClass().getName() + ": " + t.getMessage())
                                .build();
                    }
                });
    }

    @Override
    public Uni<HealthCheckResponse> callAsync(AsyncHealthCheck asyncHealthCheck) {
        Uni<HealthCheckResponse> healthCheckResponseUni = super.callAsync(asyncHealthCheck);
        return healthCheckResponseUni.runSubscriptionOn(MutinyHelper.executor(vertx));
    }
}

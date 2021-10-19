package io.quarkus.smallrye.health.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import io.quarkus.arc.AlternativePriority;
import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.health.AsyncHealthCheckFactory;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.MutinyHelper;
import io.vertx.core.Vertx;

/**
 * Quarkus specific health check factory that runs blocking and reactive
 * health checks with different executors provided by {@link MutinyHelper}.
 */
@ApplicationScoped
@AlternativePriority(1)
public class QuarkusAsyncHealthCheckFactory extends AsyncHealthCheckFactory {

    @Inject
    Vertx vertx;

    @Override
    public Uni<HealthCheckResponse> callSync(HealthCheck healthCheck) {
        Uni<HealthCheckResponse> healthCheckResponseUni = super.callSync(healthCheck);
        return BlockingOperationControl.isBlockingAllowed() ? healthCheckResponseUni
                : healthCheckResponseUni.runSubscriptionOn(MutinyHelper.blockingExecutor(vertx));
    }

    @Override
    public Uni<HealthCheckResponse> callAsync(AsyncHealthCheck asyncHealthCheck) {
        Uni<HealthCheckResponse> healthCheckResponseUni = super.callAsync(asyncHealthCheck);
        return !BlockingOperationControl.isBlockingAllowed() ? healthCheckResponseUni
                : healthCheckResponseUni.runSubscriptionOn(MutinyHelper.executor(vertx));
    }
}

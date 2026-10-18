package io.quarkus.smallrye.health.runtime;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.health.Readiness;

import io.quarkus.runtime.shutdown.ShutdownListener;

/**
 * listener that changes the readiness probe on pre-shutdown.
 *
 * Note that unless there is another preShutdown listener present
 * this will generally have no real effect, as after pre-shutdown
 * the HTTP endpoint will return service unavailable.
 */
public class ShutdownReadinessListener implements ShutdownListener {
    @Override
    public void preShutdown(ShutdownNotification notification) {
        Instance<ShutdownReadinessCheck> instance = CDI.current().select(ShutdownReadinessCheck.class,
                Readiness.Literal.INSTANCE);
        if (instance.isResolvable()) {
            instance.get().shutdown();
        }
        notification.done();
    }
}

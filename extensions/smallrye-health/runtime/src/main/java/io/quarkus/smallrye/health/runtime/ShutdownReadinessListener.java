package io.quarkus.smallrye.health.runtime;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.health.Readiness;

import io.quarkus.runtime.shutdown.ShutdownListener;

/**
 * listener that changes the readiness probe on pre-shudown
 *
 * Note that unless there is another preShutdown listener present
 * this will generally have no real effect, as after pre-shutdown
 * the HTTP endpoint will return service unavailable.
 *
 * TODO: We may want a timeout here, so the readiness probe will be down for a set timeout before shutdown continues
 */
public class ShutdownReadinessListener implements ShutdownListener {

    @Override
    public void preShutdown(ShutdownNotification notification) {
        CDI.current().select(ShutdownReadinessCheck.class, new AnnotationLiteral<Readiness>() {
        }).get().shutdown();
        notification.done();
    }
}

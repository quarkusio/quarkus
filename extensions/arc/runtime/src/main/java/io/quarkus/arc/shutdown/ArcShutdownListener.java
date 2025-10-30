package io.quarkus.arc.shutdown;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.ShutdownDelayInitiatedEvent;
import io.quarkus.runtime.shutdown.ShutdownListener;

/**
 * Fires an observable {@link ShutdownDelayInitiatedEvent} in the pre-shutdown phase if
 * {@code quarkus.shutdown.delay-enabled=true}.
 */
public class ArcShutdownListener implements ShutdownListener {
    @Override
    public void preShutdown(ShutdownNotification notification) {
        Arc.requireContainer().beanManager().getEvent().select(ShutdownDelayInitiatedEvent.class)
                .fire(new ShutdownDelayInitiatedEvent());
        notification.done();
    }
}

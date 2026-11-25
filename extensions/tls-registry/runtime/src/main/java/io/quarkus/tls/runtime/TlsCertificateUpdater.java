package io.quarkus.tls.runtime;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * A helper class that reload the TLS certificates at a configured interval.
 * When the reloaded certificates differ from the ones last notified, a {@link CertificateUpdatedEvent} is fired.
 */
public class TlsCertificateUpdater {

    private static final Logger LOGGER = Logger.getLogger(TlsCertificateUpdater.class);

    private final Vertx vertx;
    private final CopyOnWriteArrayList<Long> tasks;
    private final Event<CertificateUpdatedEvent> event;

    public TlsCertificateUpdater(Vertx vertx) {
        this.vertx = vertx;
        this.tasks = new CopyOnWriteArrayList<>();
        this.event = CDI.current().getBeanManager().getEvent().select(CertificateUpdatedEvent.class);
    }

    public void close() {
        for (Long task : tasks) {
            vertx.cancelTimer(task);
        }
        tasks.clear();
    }

    public void add(String name, VertxCertificateHolder tlsConfiguration, Duration period) {
        // Kept per task, not in the configuration, so a reload triggered elsewhere (the HTTP server reloader, the
        // application) does not hide the change from the observers.
        AtomicReference<List<Object>> notified = new AtomicReference<>(tlsConfiguration.content());
        // Skip a tick while the previous reload is still running, so a slow reload cannot overlap the next one (and
        // notify the observers out of order) nor let ticks queue up without bound.
        AtomicBoolean running = new AtomicBoolean();
        var id = vertx.setPeriodic(period.toMillis(), new Handler<Long>() {
            @Override
            public void handle(Long id) {
                if (!running.compareAndSet(false, true)) {
                    return;
                }
                vertx.executeBlocking(new Callable<Void>() {
                    @Override
                    public Void call() {
                        // Reload is most probably a blocking operation as it needs to reload the certificate from the
                        // file system. Thus, it is executed in a blocking context.
                        // Then we fire the event. This is also potentially blocking, as the consumer are invoked on the
                        // same thread.
                        if (tlsConfiguration.reload()) {
                            List<Object> content = tlsConfiguration.content();
                            if (!content.equals(notified.get())) {
                                event.fire(new CertificateUpdatedEvent(name, tlsConfiguration));
                                // Recorded only once every observer succeeded: a failing observer stops the remaining
                                // ones, so the next period must notify them again rather than miss the rotation.
                                notified.set(content);
                            }
                        }
                        return null;
                    }
                }, false).onComplete(new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> ar) {
                        running.set(false);
                        if (ar.failed()) {
                            LOGGER.warnf(ar.cause(),
                                    "Unable to apply the reloaded certificates of the TLS configuration '%s', retrying at the next reload period",
                                    name);
                        }
                    }
                });
            }
        });

        tasks.add(id);
    }

}

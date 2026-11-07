package io.quarkus.tls.runtime;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * A helper class that reload the TLS certificates at a configured interval.
 * When the certificate is reloaded, a {@link CertificateUpdatedEvent} is fired.
 */
public class TlsCertificateUpdater {

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

    public void add(String name, TlsConfiguration tlsConfiguration, Duration period) {
        // The version of the content this task last notified about. It is kept here, and not in the configuration,
        // so that a reload triggered by someone else - the HTTP server reloader, or the application itself - does
        // not hide the change from the observers of the event.
        AtomicLong notifiedVersion = new AtomicLong(tlsConfiguration.getContentVersion());
        var id = vertx.setPeriodic(period.toMillis(), new Handler<Long>() {
            @Override
            public void handle(Long id) {
                vertx.executeBlocking(new Callable<Void>() {
                    @Override
                    public Void call() {
                        // Reload is most probably a blocking operation as it needs to reload the certificate from the
                        // file system. Thus, it is executed in a blocking context.
                        // Then we fire the event. This is also potentially blocking, as the consumer are invoked on the
                        // same thread.
                        // The event is only fired when the reloaded content differs from the content the observers
                        // were last notified about: they must not swap their SSL options for nothing.
                        if (tlsConfiguration.reload() && hasNewContent(tlsConfiguration, notifiedVersion)) {
                            event.fire(new CertificateUpdatedEvent(name, tlsConfiguration));
                        }
                        return null;
                    }
                }, false);
            }
        });

        tasks.add(id);
    }

    private static boolean hasNewContent(TlsConfiguration tlsConfiguration, AtomicLong notifiedVersion) {
        long version = tlsConfiguration.getContentVersion();
        // A negative version means the configuration cannot tell whether its content changed, so notify every time.
        return version < 0 || notifiedVersion.getAndSet(version) != version;
    }

}

package io.quarkus.vertx.http.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.http.HttpServer;

/**
 * A listener that listens for certificate updates and updates the HTTP server accordingly.
 */
@Singleton
public class HttpCertificateUpdateEventListener {

    private final static Logger LOG = Logger.getLogger(HttpCertificateUpdateEventListener.class);
    private final List<CertificateUpdateRegistration> registrations = new CopyOnWriteArrayList<>();

    void register(Consumer<TlsConfiguration> tlsConfigurationConsumer, String tlsConfigurationName) {
        registrations.add(new CertificateUpdateRegistration(tlsConfigurationName) {
            @Override
            void notify(CertificateUpdatedEvent event, CountDownLatch latch) {
                try {
                    tlsConfigurationConsumer.accept(event.tlsConfiguration());
                } finally {
                    latch.countDown();
                }
            }
        });
    }

    public void register(HttpServer server, String tlsConfigurationName, String id) {
        registrations.add(new CertificateUpdateRegistration(tlsConfigurationName) {
            @Override
            void notify(CertificateUpdatedEvent event, CountDownLatch latch) {
                server.updateSSLOptions(event.tlsConfiguration().getSSLOptions())
                        .toCompletionStage().whenComplete(new BiConsumer<Boolean, Throwable>() {
                            @Override
                            public void accept(Boolean v, Throwable t) {
                                if (t == null) {
                                    LOG.infof("The TLS configuration `%s` used by the HTTP server `%s` has been updated",
                                            event.name(), id);
                                } else {
                                    LOG.warnf(t, "Failed to update TLS configuration `%s` for the HTTP server `%s`",
                                            event.name(), id);
                                }
                                latch.countDown();
                            }
                        });
            }
        });
    }

    public void onCertificateUpdate(@Observes CertificateUpdatedEvent event) throws InterruptedException {
        var eventRegistrations = new ArrayList<CertificateUpdateRegistration>();
        for (CertificateUpdateRegistration registration : registrations) {
            if (registration.appliesTo(event.name())) {
                eventRegistrations.add(registration);
            }
        }
        if (!eventRegistrations.isEmpty()) {
            CountDownLatch latch = new CountDownLatch(eventRegistrations.size());
            for (CertificateUpdateRegistration registration : eventRegistrations) {
                registration.notify(event, latch);
            }
            if (!latch.await(30, TimeUnit.SECONDS)) {
                LOG.warn("Certificate update did not complete within 30 seconds");
            }
        }
    }

    private static abstract class CertificateUpdateRegistration {

        private final String tlsConfigurationName;

        private CertificateUpdateRegistration(String tlsConfigurationName) {
            this.tlsConfigurationName = tlsConfigurationName;
        }

        abstract void notify(CertificateUpdatedEvent event, CountDownLatch latch);

        private boolean appliesTo(String other) {
            return tlsConfigurationName.equalsIgnoreCase(other);
        }

    }
}

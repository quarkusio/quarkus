package io.quarkus.vertx.http.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.vertx.core.http.HttpServer;

/**
 * A listener that listens for certificate updates and updates the HTTP server accordingly.
 */
@Singleton
public class HttpCertificateUpdateEventListener {

    private final static Logger LOG = Logger.getLogger(HttpCertificateUpdateEventListener.class);
    private final List<ServerRegistration> servers = new CopyOnWriteArrayList<>();

    record ServerRegistration(HttpServer server, String tlsConfigurationName, String id) {

    }

    public void register(HttpServer server, String tlsConfigurationName, String id) {
        servers.add(new ServerRegistration(server, tlsConfigurationName, id));
    }

    public void onCertificateUpdate(@Observes CertificateUpdatedEvent event) throws InterruptedException {
        // Retrieve the server that uses the updated TLS configuration
        List<ServerRegistration> registrations = new ArrayList<>();
        for (ServerRegistration server : servers) {
            if (server.tlsConfigurationName.equalsIgnoreCase(event.name())) {
                registrations.add(server);
            }
        }
        CountDownLatch latch = new CountDownLatch(registrations.size());
        for (ServerRegistration server : registrations) {
            server.server.updateSSLOptions(event.tlsConfiguration().getSSLOptions())
                    .toCompletionStage().whenComplete(new BiConsumer<Boolean, Throwable>() {
                        @Override
                        public void accept(Boolean v, Throwable t) {
                            if (t == null) {
                                LOG.infof("The TLS configuration `%s` used by the HTTP server `%s` has been updated",
                                        event.name(), server.id);
                            } else {
                                LOG.warnf(t, "Failed to update TLS configuration `%s` for the HTTP server `%s`",
                                        event.name(),
                                        server.id);
                            }
                            latch.countDown();
                        }
                    });
        }

        latch.await();
    }
}

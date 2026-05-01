package io.quarkus.oidc.common.runtime;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class CertificateUpdateEventListener {

    private record WebClientRegistration(String tlsConfigName, WebClient webClient, String clientUser) {
    }

    private static final Logger LOG = Logger.getLogger(CertificateUpdateEventListener.class);

    private final List<WebClientRegistration> webClientRegistrations;

    CertificateUpdateEventListener() {
        this.webClientRegistrations = new CopyOnWriteArrayList<>();
    }

    void onCertificateUpdate(@Observes CertificateUpdatedEvent event) throws InterruptedException {
        if (!webClientRegistrations.isEmpty()) {
            var registrationsToUpdate = webClientRegistrations.stream().filter(r -> r.tlsConfigName.equals(event.name()))
                    .toList();
            if (!registrationsToUpdate.isEmpty()) {
                CountDownLatch latch = new CountDownLatch(registrationsToUpdate.size());
                for (var registration : registrationsToUpdate) {
                    registration.webClient.updateSSLOptions(event.tlsConfiguration().getSSLOptions())
                            .subscribe().with(
                                    ignored -> {
                                        LOG.infof("The TLS configuration `%s` used by the WebClient of the %s has been updated",
                                                event.name(), registration.clientUser);
                                        latch.countDown();
                                    },
                                    throwable -> {
                                        LOG.warnf(throwable,
                                                "Failed to update TLS configuration `%s` for the WebClient of the %s",
                                                event.name(), registration.clientUser);
                                        latch.countDown();
                                    });
                }

                latch.await();
            }
        }
    }

    Runnable registerWebClient(String tlsConfigName, WebClient webClient, String clientUser) {
        var registration = new WebClientRegistration(tlsConfigName, webClient, clientUser);
        webClientRegistrations.add(registration);
        return () -> webClientRegistrations.remove(registration);
    }
}

package io.quarkus.oidc.common.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.smallrye.mutiny.Uni;
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

    void onCertificateUpdate(@Observes CertificateUpdatedEvent event) {
        if (!webClientRegistrations.isEmpty()) {
            var registrationsToUpdate = webClientRegistrations.stream().filter(r -> r.tlsConfigName.equals(event.name()))
                    .toList();
            if (!registrationsToUpdate.isEmpty()) {
                List<Uni<Void>> updates = new ArrayList<>();
                for (var registration : registrationsToUpdate) {
                    Uni<Void> updateUni = registration.webClient
                            .updateSSLOptions(event.tlsConfiguration().getClientSSLOptions())
                            .onItem().invoke(new Runnable() {
                                @Override
                                public void run() {
                                    LOG.infof("The TLS configuration `%s` used by the WebClient of the %s has been updated",
                                            event.name(), registration.clientUser);
                                }
                            })
                            .onFailure().recoverWithUni(new Function<Throwable, Uni<? extends Boolean>>() {
                                @Override
                                public Uni<? extends Boolean> apply(Throwable throwable) {
                                    LOG.warnf(throwable,
                                            "Failed to update TLS configuration `%s` for the WebClient of the %s",
                                            event.name(), registration.clientUser);
                                    return Uni.createFrom().item(false);
                                }
                            })
                            .replaceWithVoid();
                    updates.add(updateUni);
                }

                Uni.join().all(updates).andFailFast().await().atMost(Duration.ofSeconds(30));
            }
        }
    }

    Runnable registerWebClient(String tlsConfigName, WebClient webClient, String clientUser) {
        var registration = new WebClientRegistration(tlsConfigName, webClient, clientUser);
        webClientRegistrations.add(registration);
        return () -> webClientRegistrations.remove(registration);
    }
}

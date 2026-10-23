package io.quarkus.kafka.client.tls;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.tls.CertificateUpdatedEvent;

/**
 * Propagates TLS registry reloads to the Kafka clients using the reloaded TLS configuration.
 * <p>
 * Kafka instantiates {@link QuarkusKafkaSslEngineFactory} reflectively, so the factories register themselves here when
 * configured and unregister when closed. When a {@link CertificateUpdatedEvent} is fired (periodic
 * {@code quarkus.tls.<name>.reload-period}, Let's Encrypt renewal, or manually by the application), each factory using
 * that TLS configuration rebuilds its {@link javax.net.ssl.SSLContext} once. New connections opened by the client use
 * the new material; established connections are not affected.
 */
@ApplicationScoped
public class KafkaCertificateUpdateEventListener {

    private final List<QuarkusKafkaSslEngineFactory> factories = new CopyOnWriteArrayList<>();

    /**
     * @return a callback removing the registration
     */
    Runnable register(QuarkusKafkaSslEngineFactory factory) {
        factories.add(factory);
        return () -> factories.remove(factory);
    }

    void onCertificateUpdate(@Observes CertificateUpdatedEvent event) {
        for (QuarkusKafkaSslEngineFactory factory : factories) {
            if (event.name().equals(factory.tlsConfigName())) {
                factory.reload(event.tlsConfiguration());
            }
        }
    }

    int registrations() {
        return factories.size();
    }
}

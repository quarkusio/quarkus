package io.quarkus.it.kafka.ssl;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;

@ApplicationScoped
@Path("/ssl/certificate-reload")
public class CertificateReloadResource {

    @Inject
    TlsConfigurationRegistry registry;

    @Inject
    Event<CertificateUpdatedEvent> certificateUpdatedEvent;

    private final AtomicInteger reloadCount = new AtomicInteger();

    void onCertificateUpdate(@Observes CertificateUpdatedEvent event) {
        reloadCount.incrementAndGet();
    }

    @POST
    public int reload(@QueryParam("name") String name) {
        TlsConfiguration config = registry.get(name).orElseThrow();
        certificateUpdatedEvent.fire(new CertificateUpdatedEvent(name, config));
        return reloadCount.get();
    }
}

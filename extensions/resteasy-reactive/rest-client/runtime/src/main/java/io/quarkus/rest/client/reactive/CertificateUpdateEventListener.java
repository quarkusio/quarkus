package io.quarkus.rest.client.reactive;

import java.util.List;

import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;

import io.quarkus.rest.client.reactive.runtime.RestClientRecorder;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientAgent;

public class CertificateUpdateEventListener {

    private static final Logger LOG = Logger.getLogger(CertificateUpdateEventListener.class);

    public void onCertificateUpdate(@Observes CertificateUpdatedEvent event) {
        String updatedTlsConfigurationName = event.name();
        TlsConfiguration updatedTlsConfiguration = event.tlsConfiguration();
        List<HttpClient> httpClients = RestClientRecorder.clientsUsingTlsConfig(updatedTlsConfigurationName);
        if (!httpClients.isEmpty()) {
            for (HttpClient httpClient : httpClients) {
                ((HttpClientAgent) httpClient)
                        .updateSSLOptions(updatedTlsConfiguration.getClientSSLOptions())
                        .onComplete(event1 -> {
                            if (event1.succeeded()) {
                                if (event1.result()) {
                                    LOG.infof(
                                            "Certificate reloaded for the REST client(s) using the TLS configuration (bucket) name '%s'",
                                            updatedTlsConfigurationName);
                                } else {
                                    LOG.warnf("Certificate reload skipped for the TLS configuration (bucket) name '%s'",
                                            updatedTlsConfigurationName);
                                }
                            } else {
                                LOG.errorf(event1.cause(),
                                        "Certificate reload failed  using the TLS configuration (bucket) name '%s'",
                                        event1.cause(), updatedTlsConfigurationName);
                            }
                        });
            }

        }
    }
}

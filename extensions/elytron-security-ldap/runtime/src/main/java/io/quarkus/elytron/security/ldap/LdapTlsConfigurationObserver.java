package io.quarkus.elytron.security.ldap;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkus.tls.CertificateUpdatedEvent;

@Singleton
public class LdapTlsConfigurationObserver {

    void onCertificateUpdated(@Observes CertificateUpdatedEvent event) {
        QuarkusLdapSocketFactory.reload(event.name(), event.tlsConfiguration());
    }
}

package io.quarkus.elytron.security.ldap;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.tls.CertificateUpdatedEvent;

@Singleton
public class LdapTlsConfigurationObserver {

    private static final Logger LOG = Logger.getLogger(LdapTlsConfigurationObserver.class);

    void onCertificateUpdated(@Observes CertificateUpdatedEvent event) {
        try {
            QuarkusLdapSocketFactory.reload(event.name(), event.tlsConfiguration());
        } catch (RuntimeException e) {
            // an exception would stop the other observers of the event, such as the HTTP server reloader
            LOG.warnf(e, "Unable to reload the TLS configuration '%s' for the LDAP connections, the previous one is kept",
                    event.name());
        }
    }
}

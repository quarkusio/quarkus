package io.quarkus.vertx.http.runtime.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.security.credential.CertificateCredential;
import io.quarkus.security.identity.SecurityIdentity;

@RequestScoped
public class CertificateCredentialProducer {
    private static final Logger LOG = Logger.getLogger(CertificateCredentialProducer.class);
    @Inject
    Instance<SecurityIdentity> identity;

    @Produces
    @RequestScoped
    CertificateCredential certificateCredential() {
        CertificateCredential cred = identity.isResolvable() ? identity.get().getCredential(CertificateCredential.class) : null;
        if (cred == null) {
            LOG.trace("CertificateCredential is null");
            cred = new CertificateCredential(null);
        }
        return cred;
    }
}

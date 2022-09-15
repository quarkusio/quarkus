package io.quarkus.extest.runtime.beans;

import java.security.interfaces.DSAPublicKey;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.jboss.logging.Logger;

/**
 * Producer of DSAPublicKey
 */
@ApplicationScoped
public class PublicKeyProducer {
    private static final Logger log = Logger.getLogger("PublicKeyProducer");
    private DSAPublicKey publicKey;

    public PublicKeyProducer() {
    }

    @Produces
    public DSAPublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(DSAPublicKey publicKey) {
        log.debugf("setPublicKey, key=%s", publicKey);
        this.publicKey = publicKey;
    }
}

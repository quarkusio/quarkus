package io.quarkus.extest.runtime.subst;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ObjectSubstitution;

/**
 * Example ObjectSubstitution for DSA public key substitution
 * The DSA key provider is the SUN provider enabled by default in Graal
 */
public class DSAPublicKeyObjectSubstitution implements ObjectSubstitution<DSAPublicKey, KeyProxy> {
    private static final Logger log = Logger.getLogger("DSAPublicKeyObjectSubstitution");

    @Override
    public KeyProxy serialize(DSAPublicKey obj) {
        log.info("DSAPublicKeyObjectSubstitution.serialize");
        byte[] encoded = obj.getEncoded();
        KeyProxy proxy = new KeyProxy();
        proxy.setContent(encoded);
        return proxy;
    }

    @Override
    public DSAPublicKey deserialize(KeyProxy obj) {
        log.info("DSAPublicKeyObjectSubstitution.deserialize");
        byte[] encoded = obj.getContent();
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encoded);
        DSAPublicKey dsaPublicKey = null;
        try {
            KeyFactory kf = KeyFactory.getInstance("DSA");
            dsaPublicKey = (DSAPublicKey) kf.generatePublic(publicKeySpec);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return dsaPublicKey;
    }
}

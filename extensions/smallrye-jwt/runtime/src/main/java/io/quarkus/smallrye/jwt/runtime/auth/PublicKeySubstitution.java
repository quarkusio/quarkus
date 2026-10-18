package io.quarkus.smallrye.jwt.runtime.auth;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import io.quarkus.runtime.ObjectSubstitution;

public class PublicKeySubstitution implements ObjectSubstitution<RSAPublicKey, PublicKeyProxy> {
    @Override
    public PublicKeyProxy serialize(RSAPublicKey obj) {
        byte[] encoded = obj.getEncoded();
        PublicKeyProxy proxy = new PublicKeyProxy();
        proxy.setContent(encoded);
        return proxy;
    }

    @Override
    public RSAPublicKey deserialize(PublicKeyProxy obj) {
        byte[] encoded = obj.getContent();
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encoded);
        RSAPublicKey rsaPubKey = null;
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            rsaPubKey = (RSAPublicKey) kf.generatePublic(publicKeySpec);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return rsaPubKey;
    }
}

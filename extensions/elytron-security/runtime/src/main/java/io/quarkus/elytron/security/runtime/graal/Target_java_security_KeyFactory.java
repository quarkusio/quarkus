package io.quarkus.elytron.security.runtime.graal;

import java.security.KeyFactory;
import java.security.KeyFactorySpi;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import sun.security.rsa.RSAKeyFactory;
import sun.security.rsa.SunRsaSign;

/**
 * Override the {@linkplain KeyFactory#getInstance(String)} to deal with creating the KeyFactory directly for "RSA" algorithm
 */
//@TargetClass(KeyFactory.class)
public final class Target_java_security_KeyFactory {
    static class QuarkusKeyFactory extends KeyFactory {
        QuarkusKeyFactory(KeyFactorySpi keyFacSpi, Provider provider, String algorithm) {
            super(keyFacSpi, provider, algorithm);
        }
    }

    //@Substitute
    public static KeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm.equals("RSA")) {
            SunRsaSign rsaProvider = new SunRsaSign();
            KeyFactorySpi keyFactorySpi = new RSAKeyFactory();
            return new QuarkusKeyFactory(keyFactorySpi, rsaProvider, algorithm);
        }
        return null;
    }
}

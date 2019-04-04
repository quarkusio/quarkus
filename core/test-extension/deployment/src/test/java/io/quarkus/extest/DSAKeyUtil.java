package io.quarkus.extest;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility main to validate that the DSA key provider is the default SUN provider enabled by Graal
 * and to generate a DSA public key encoded string
 */
public class DSAKeyUtil {
    public static void main(String[] args) throws Exception {
        Provider provider = Security.getProvider("SUN");
        System.out.println(provider.getInfo());
        for (Provider.Service service : provider.getServices()) {
            System.out.println(service);
        }

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA");
        kpg.initialize(2048);
        System.out.println("DSA.provider: " + kpg.getProvider());
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        System.out.println("DSA.provider: " + keyFactory.getProvider());

        KeyPair pair = kpg.genKeyPair();
        DSAPublicKey publicKey = (DSAPublicKey) pair.getPublic();
        byte[] encoded = publicKey.getEncoded();
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encoded);
        DSAPublicKey publicKey1 = (DSAPublicKey) keyFactory.generatePublic(publicKeySpec);
        System.out.printf("keys are equal: %s\n", publicKey1.equals(publicKey));

        String base64 = Base64.getEncoder().encodeToString(encoded);
        System.out.println(base64);
    }

}

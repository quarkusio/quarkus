package io.quarkus.it.bouncycastle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateKey;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

@Path("/jca")
public class BouncyCastleEndpoint {

    @GET
    @Path("createjceks")
    public String createjceks() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(null, null);

        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        keyGen.init(56);
        ;
        Key key = keyGen.generateKey();

        keyStore.setKeyEntry("secret", key, "password".toCharArray(), null);

        OutputStream os = new FileOutputStream("output.jceks");
        keyStore.store(os, "password".toCharArray());
        os.close();
        return "";
    }

    @GET
    @Path("jceks")
    public String jceks() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(new FileInputStream("output.jceks"), "password".toCharArray());

        Key key = keyStore.getKey("secret", "password".toCharArray());

        return key.toString();
    }

    @GET
    @Path("listProviders")
    public String listProviders() {
        return Arrays.asList(Security.getProviders()).stream()
                .filter(p -> (p.getName().equals("BC") || p.getName().equals("SunPKCS11")))
                .map(p -> p.getName()).collect(Collectors.joining(","));
    }

    @GET
    @Path("SHA256withRSAandMGF1")
    public String checkSHA256withRSAandMGF1() throws Exception {
        // This algorithm name is only supported with BC, Java (11+) equivalent is `RSASSA-PSS`
        Signature.getInstance("SHA256withRSAandMGF1", "BC");
        return "success";
    }

    @GET
    @Path("generateEcKeyPair")
    public String generateEcKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        keyPairGenerator.generateKeyPair();
        return "success";
    }

    @GET
    @Path("generateEcDsaKeyPair")
    public String generateEcDsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyPairGenerator.generateKeyPair();
        return "success";
    }

    @GET
    @Path("generateRsaKeyPair")
    public String generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.generateKeyPair();
        return "success";
    }

    @GET
    @Path("checkAesCbcPKCS7PaddingCipher")
    public String checkAesCbcPKCS7PaddingCipher() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        return cipher.getAlgorithm();
    }

    @GET
    @Path("readEcPrivatePemKey")
    public String readEcPrivatePemKey() throws Exception {
        KeyFactory factory = KeyFactory.getInstance("EC", "BC");

        try (InputStream pemKeyInputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("ecPrivateKey.pem")) {
            PemReader pemReader = new PemReader(new InputStreamReader(pemKeyInputStream));
            PemObject pemObject = pemReader.readPemObject();

            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(pemObject.getContent());
            BCECPrivateKey ecPrivateKey = (BCECPrivateKey) factory.generatePrivate(privKeySpec);

            return ecPrivateKey.getD() != null ? "success" : "failure";
        }
    }

    @GET
    @Path("readRsaPrivatePemKey")
    public String readRsaPrivatePemKey() throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA", "BC");

        try (InputStream pemKeyInputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("rsaPrivateKey.pem")) {
            PemReader pemReader = new PemReader(new InputStreamReader(pemKeyInputStream));
            PemObject pemObject = pemReader.readPemObject();

            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(pemObject.getContent());
            BCRSAPrivateKey ecPrivateKey = (BCRSAPrivateKey) factory.generatePrivate(privKeySpec);

            return ecPrivateKey.getPrivateExponent() != null ? "success" : "failure";
        }
    }
}

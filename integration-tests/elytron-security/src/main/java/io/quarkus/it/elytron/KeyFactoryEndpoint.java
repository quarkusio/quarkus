package io.quarkus.it.elytron;

import java.security.KeyFactory;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.jboss.logging.Logger;

@Path("/jca")
public class KeyFactoryEndpoint {
    private static final Logger log = Logger.getLogger(KeyFactoryEndpoint.class);

    @GET
    @Path("listProviders")
    public String listProviders() {
        StringBuilder result = new StringBuilder();
        final Provider[] providerList = Security.getProviders();
        for (Provider provider : providerList) {
            result.append(provider.getName());
        }
        log.infof("Found providers: %s", result);
        return result.toString();
    }

    @GET
    @Path("decodeRSAKey")
    public String decodeRSAKey(@QueryParam("pemEncoded") String pemEncoded) throws Exception {
        byte[] encodedBytes = Base64.getDecoder().decode(pemEncoded);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        log.infof("Loaded RSA.KeyFactory: %s", kf);
        PublicKey pk = kf.generatePublic(spec);
        return pk.getAlgorithm();
    }

    @GET
    @Path("verifyRSASig")
    public boolean verifyRSASig(@QueryParam("msg") String msg, @QueryParam("publicKey") String publicKey,
            @QueryParam("sig") String sig) throws Exception {
        byte[] encodedBytes = Base64.getDecoder().decode(publicKey);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        log.infof("Loaded RSA.KeyFactory: %s", kf);
        PublicKey pk = kf.generatePublic(spec);

        Signature sha256withRSA = Signature.getInstance("SHA256withRSA");
        log.infof("Loaded SHA256withRSA: %s", sha256withRSA);
        //log.infof("Loaded SHA256withRSA: %s, %s", sha256withRSA, sha256withRSA.getProvider());
        sha256withRSA.initVerify(pk);
        log.info("Initialized SHA256withRSA");

        return true;
    }

    @GET
    @Path("SHA256withRSAandMGF1")
    public String checkSHA256withRSAandMGF1() {
        // This algorithm name is only supported with BC, Java (11+) equivalent is `RSASSA-PSS`
        try {
            Signature.getInstance("SHA256withRSAandMGF1");
            return "success";
        } catch (Throwable t) {
            return "failure";
        }
    }
}

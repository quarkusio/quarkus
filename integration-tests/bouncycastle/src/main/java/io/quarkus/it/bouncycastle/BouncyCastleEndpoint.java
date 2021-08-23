package io.quarkus.it.bouncycastle;

import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.Signature;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/jca")
public class BouncyCastleEndpoint {

    @GET
    @Path("listProviders")
    public String listProviders() {
        return Arrays.asList(Security.getProviders()).stream()
                .filter(p -> p.getName().equals("BC"))
                .map(p -> p.getName()).collect(Collectors.joining());
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
    @Path("generateRsaKeyPair")
    public String generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.generateKeyPair();
        return "success";
    }
}

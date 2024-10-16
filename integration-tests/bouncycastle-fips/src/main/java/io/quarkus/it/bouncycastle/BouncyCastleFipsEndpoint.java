package io.quarkus.it.bouncycastle;

import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.crypto.KeyGenerator;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.EntropySourceProvider;
import org.bouncycastle.crypto.fips.FipsDRBG;
import org.bouncycastle.crypto.fips.FipsUnapprovedOperationError;
import org.bouncycastle.crypto.util.BasicEntropySourceProvider;
import org.bouncycastle.util.Strings;

@Path("/jca")
public class BouncyCastleFipsEndpoint {

    @GET
    @Path("listProviders")
    public String listProviders() {
        return Arrays.asList(Security.getProviders()).stream()
                .filter(p -> p.getName().equals("BCFIPS"))
                .map(p -> p.getName()).collect(Collectors.joining());
    }

    @GET
    @Path("SHA256withRSAandMGF1")
    public String checkSHA256withRSAandMGF1() throws Exception {
        // This algorithm name is only supported with BC, Java (11+) equivalent is `RSASSA-PSS`
        Signature.getInstance("SHA256withRSAandMGF1", "BCFIPS");
        return "success";
    }

    @GET
    @Path("fipsmode")
    public String confirmFipsMode() throws Exception {
        // https://www.bouncycastle.org/fips-java/BCFipsIn100.pdf

        // Ensure that only approved algorithms and key sizes for FIPS-140-3.
        CryptoServicesRegistrar.setApprovedOnlyMode(true);
        // Set Secure Random to be compliant
        EntropySourceProvider entSource = new BasicEntropySourceProvider(new SecureRandom(), true);
        FipsDRBG.Builder drgbBldr = FipsDRBG.SHA512
                .fromEntropySource(entSource)
                .setSecurityStrength(256)
                .setEntropyBitsRequired(256);
        CryptoServicesRegistrar.setSecureRandom(drgbBldr.build(Strings.toByteArray("axs"), true));

        // Validates FIPS Mode enabled and enforced correctly with Unapproved Key Generation
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA512", "BCFIPS");

        try {
            keyGenerator.init(256);
            return "HMAC SHA-512 initialization should not work when FIPS enabled.";
        } catch (FipsUnapprovedOperationError ex) {
            return "HMAC SHA-512 initialization does not work when FIPS enabled.";
        } catch (Exception exception) {
            return exception.getClass().getName();
        }
    }
}

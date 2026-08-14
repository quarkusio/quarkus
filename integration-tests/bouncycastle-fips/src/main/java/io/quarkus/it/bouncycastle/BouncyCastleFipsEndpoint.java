package io.quarkus.it.bouncycastle;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
    @Path("poly1305")
    public String poly1305() throws Exception {
        // Reproducer for https://github.com/quarkusio/quarkus/issues/56005:
        // resolving the POLY1305-AES MAC forces ProvPoly1305's AES engine creator to be reachable, which drags
        // org.bouncycastle.crypto.general.Poly1305's statically built AESEngine (and its AESWorkingBuffer) into the
        // native image heap. Since AESWorkingBuffer is run-time initialized, this fails the native build unless the
        // Poly1305 holder class is run-time initialized too.
        // POLY1305-AES is a general (non-FIPS-approved) algorithm. BC FIPS approved-only mode is thread-local
        // and one-way (cannot be reset to false), so a fresh thread is needed to avoid inheriting approved-only
        // mode set by the fipsmode endpoint on a reused worker thread.
        AtomicReference<String> result = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                Mac mac = Mac.getInstance("POLY1305-AES", "BCFIPS");
                mac.init(new SecretKeySpec(new byte[32], "POLY1305"), new IvParameterSpec(new byte[16]));
                mac.update("quarkus".getBytes(StandardCharsets.UTF_8));
                result.set(mac.doFinal().length == 16 ? "success" : "unexpected");
            } catch (Exception e) {
                result.set(e.getClass().getName() + ": " + e.getMessage());
            }
        });
        worker.start();
        worker.join();
        return result.get();
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

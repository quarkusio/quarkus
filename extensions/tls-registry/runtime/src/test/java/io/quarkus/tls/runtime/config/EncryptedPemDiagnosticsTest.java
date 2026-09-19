package io.quarkus.tls.runtime.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.security.Provider;
import java.security.Security;
import java.util.Base64;

import javax.crypto.EncryptedPrivateKeyInfo;

import org.junit.jupiter.api.Test;

public class EncryptedPemDiagnosticsTest {

    private static final Path KEY = Path.of("certs", "server.key");

    @Test
    public void keyThatIsNotEncrypted() {
        String message = EncryptedPemDiagnostics.explain(KEY,
                "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBg\n-----END PRIVATE KEY-----\n");
        assertTrue(message.startsWith("Unable to decrypt the key file " + KEY), message);
        assertTrue(message.contains("not an encrypted PKCS#8 key"), message);
    }

    @Test
    public void malformedEncryptedKey() {
        String message = EncryptedPemDiagnostics.explain(KEY,
                "-----BEGIN ENCRYPTED PRIVATE KEY-----\nnot base64!!\n-----END ENCRYPTED PRIVATE KEY-----\n");
        assertTrue(message.contains("malformed"), message);
    }

    @Test
    public void wrongPasswordOrUnsupportedAlgorithm() throws Exception {
        assumeTrue(fipsProviderNames().isEmpty(), "a FIPS provider is installed");
        String message = EncryptedPemDiagnostics.explain(KEY, encryptedKey("PBEWithMD5AndDES"));
        assertTrue(message.contains("wrong password"), message);
        assertTrue(message.contains("PBEWithMD5AndDES"), message);
    }

    @Test
    public void fipsProvider() throws Exception {
        Provider provider = new Provider("TestFIPS", "1.0", "a provider whose name marks a FIPS environment") {
        };
        Security.addProvider(provider);
        try {
            String message = EncryptedPemDiagnostics.explain(KEY, encryptedKey("PBEWithMD5AndDES"));
            assertTrue(message.contains("FIPS providers (TestFIPS)"), message);
            assertTrue(message.contains("PKCS12"), message);
        } finally {
            Security.removeProvider(provider.getName());
        }
    }

    private static String encryptedKey(String algorithm) throws Exception {
        byte[] der = new EncryptedPrivateKeyInfo(algorithm, new byte[] { 1, 2, 3, 4 }).getEncoded();
        return "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(der)
                + "\n-----END ENCRYPTED PRIVATE KEY-----\n";
    }

    private static String fipsProviderNames() {
        StringBuilder names = new StringBuilder();
        for (Provider provider : Security.getProviders()) {
            if (provider.getName().toUpperCase().contains("FIPS")) {
                names.append(provider.getName()).append(' ');
            }
        }
        return names.toString().trim();
    }
}

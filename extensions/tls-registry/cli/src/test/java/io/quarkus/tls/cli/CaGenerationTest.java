package io.quarkus.tls.cli;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.certs.ca.CaGenerator;

public class CaGenerationTest {

    // Disabled on Windows: CaGenerator does not close the keystore output stream
    // (https://github.com/smallrye/smallrye-certificate-generator/issues/92), which keeps the .p12 file
    // locked so @TempDir cleanup fails there. Revert this annotation once that issue is fixed.
    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testCaGeneration(@TempDir Path tempDir) throws Exception {
        File caFile = tempDir.resolve("quarkus-dev-root-ca.pem").toFile();
        File pkFile = tempDir.resolve("quarkus-dev-root-key.pem").toFile();
        File keystoreFile = tempDir.resolve("quarkus-dev-keystore.p12").toFile();

        GenerateCACommand command = new GenerateCACommand();
        command.generateCA(caFile, pkFile, keystoreFile);

        Assertions.assertTrue(caFile.isFile());
        Assertions.assertTrue(pkFile.isFile());
        Assertions.assertTrue(keystoreFile.isFile());

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            ks.load(fis, "quarkus".toCharArray());
        }
        X509Certificate cert = (X509Certificate) ks.getCertificate(CaGenerator.KEYSTORE_CERT_ENTRY);
        Assertions.assertNotNull(cert);
        // The generated CA must carry a well-formed X.500 subject.
        Assertions.assertTrue(cert.getSubjectX500Principal().getName().contains("CN=quarkus-dev-root-ca"));
    }
}

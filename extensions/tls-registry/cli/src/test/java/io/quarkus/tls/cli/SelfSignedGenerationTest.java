package io.quarkus.tls.cli;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.KeyStore;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SelfSignedGenerationTest {

    @AfterAll
    static void cleanup() {
        // File generated during the generation.
        File file = new File(".env");
        if (file.isFile()) {
            file.delete();
        }
    }

    @Test
    public void testSelfSignedGeneration() throws Exception {
        GenerateCertificateCommand command = new GenerateCertificateCommand();
        command.name = "test";
        command.renew = true;
        command.selfSigned = true;
        command.directory = Path.of("target");
        command.password = "password";
        command.call();

        File file = new File("target/test-keystore.p12");
        Assertions.assertTrue(file.exists());

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(file)) {
            ks.load(fis, "password".toCharArray());
            Assertions.assertNotNull(ks.getCertificate("test"));
            Assertions.assertNotNull(ks.getKey("test", "password".toCharArray()));
        }
    }
}

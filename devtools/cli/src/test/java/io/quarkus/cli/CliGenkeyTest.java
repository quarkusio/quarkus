package io.quarkus.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CliGenkeyTest {
    static Path workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().resolve("target/test-project");
    static Path privateKey = workspaceRoot.resolve("src/main/resources/private-key.pem");
    static Path publicKey = workspaceRoot.resolve("src/main/resources/public-key.pem");

    @BeforeEach
    public void clean() throws IOException {
        if (Files.exists(privateKey)) {
            Files.delete(privateKey);
        }
        if (Files.exists(publicKey)) {
            Files.delete(publicKey);
        }
    }

    @Test
    public void testCommandRsa() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "genkey");
        Assertions.assertTrue(Files.exists(privateKey));
        Assertions.assertTrue(Files.exists(publicKey));
        List<String> privateKeyContents = Files.readAllLines(privateKey);
        Assertions.assertTrue(privateKeyContents.size() >= 3);
        Assertions.assertEquals("-----BEGIN PRIVATE KEY-----", privateKeyContents.get(0));
        verifyKeyParameters(privateKeyContents, "RSA", 2048);
        List<String> publicKeyContents = Files.readAllLines(publicKey);
        Assertions.assertTrue(publicKeyContents.size() >= 3);
        Assertions.assertEquals("-----BEGIN PUBLIC KEY-----", publicKeyContents.get(0));
        Assertions.assertTrue(result.stdout.contains("Public and private keys created in"));

        // now run it again, it should complain but leave the files alone
        result = CliDriver.execute(workspaceRoot, "genkey");
        Assertions.assertTrue(Files.exists(privateKey));
        Assertions.assertTrue(Files.exists(publicKey));
        List<String> privateKeyContents2 = Files.readAllLines(privateKey);
        Assertions.assertEquals(privateKeyContents, privateKeyContents2);
        List<String> publicKeyContents2 = Files.readAllLines(publicKey);
        Assertions.assertEquals(publicKeyContents, publicKeyContents2);
        Assertions.assertTrue(result.stdout.contains("Public and private keys already exist in"));

        // now run it again in force, to override the files
        result = CliDriver.execute(workspaceRoot, "genkey", "--force");
        Assertions.assertTrue(Files.exists(privateKey));
        Assertions.assertTrue(Files.exists(publicKey));
        privateKeyContents2 = Files.readAllLines(privateKey);
        Assertions.assertNotEquals(privateKeyContents, privateKeyContents2);
        publicKeyContents2 = Files.readAllLines(publicKey);
        Assertions.assertNotEquals(publicKeyContents, publicKeyContents2);
        Assertions.assertTrue(result.stdout.contains("Public and private keys created in"));

        // run it for 4096
        result = CliDriver.execute(workspaceRoot, "genkey", "--force", "--size", "4096");
        Assertions.assertTrue(Files.exists(privateKey));
        verifyKeyParameters(Files.readAllLines(privateKey), "RSA", 4096);

        // run it for EC
        result = CliDriver.execute(workspaceRoot, "genkey", "--force", "--algo", "EC");
        Assertions.assertTrue(Files.exists(privateKey));
        verifyKeyParameters(Files.readAllLines(privateKey), "EC", 256);

        // run it for EC/512
        result = CliDriver.execute(workspaceRoot, "genkey", "--force", "--algo", "EC", "--size", "512");
        Assertions.assertTrue(Files.exists(privateKey));
        verifyKeyParameters(Files.readAllLines(privateKey), "EC", 512);
    }

    private void verifyKeyParameters(List<String> privateKeyContents, String algo, int keySize) {
        StringBuilder key = new StringBuilder();
        for (int i = 1; i < privateKeyContents.size() - 1; i++) {
            key.append(privateKeyContents.get(i));
        }
        byte[] keyBytes = Base64.getDecoder().decode(key.toString());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        try {
            PrivateKey privateKey = KeyFactory.getInstance(algo).generatePrivate(keySpec);
            int observedSize;
            if (algo.equals("RSA")) {
                observedSize = ((RSAPrivateKey) privateKey).getModulus().bitLength();
            } else {
                observedSize = ((ECPrivateKey) privateKey).getS().bitLength();
            }
            Assertions.assertTrue(observedSize > keySize - 8 && observedSize <= keySize,
                    "Key size is not valid: " + observedSize + " (expected " + keySize + ")");
        } catch (InvalidKeySpecException e) {
            Assertions.fail("Key is not of the expected algo: " + algo, e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

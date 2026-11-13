package io.quarkus.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CliJwtGenerateKeyTest {
    static Path workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().resolve("target/test-project");
    static Path privateKey = workspaceRoot.resolve("src/main/resources/privateKey.pem");
    static Path publicKey = workspaceRoot.resolve("src/main/resources/publicKey.pem");
    static Path appProps = workspaceRoot.resolve("src/main/resources/application.properties");
    static Path encPrivateKey = workspaceRoot.resolve("src/main/resources/encPrivateKey.pem");
    static Path encPublicKey = workspaceRoot.resolve("src/main/resources/encPublicKey.pem");
    static Path keysDir = workspaceRoot.resolve("src/main/resources/keys");

    @BeforeEach
    public void clean() throws IOException {
        if (Files.exists(privateKey)) {
            Files.delete(privateKey);
        }
        if (Files.exists(publicKey)) {
            Files.delete(publicKey);
        }
        if (Files.exists(appProps)) {
            Files.delete(appProps);
        }
        Files.deleteIfExists(encPrivateKey);
        Files.deleteIfExists(encPublicKey);
        Files.deleteIfExists(keysDir.resolve("privateKey.pem"));
        Files.deleteIfExists(keysDir.resolve("publicKey.pem"));
        Files.deleteIfExists(keysDir);
    }

    @Test
    public void testCommandRsa() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "jwt", "generate-key");
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

        // verify application.properties was created with JWT config
        Assertions.assertTrue(Files.exists(appProps));
        String config = Files.readString(appProps);
        Assertions.assertTrue(config.contains("mp.jwt.verify.publickey.location=publicKey.pem"));
        Assertions.assertTrue(config.contains("smallrye.jwt.sign.key.location=privateKey.pem"));
        Assertions.assertFalse(config.contains("smallrye.jwt.encrypt.key.location"));
        Assertions.assertFalse(config.contains("mp.jwt.decrypt.key.location"));
        Assertions.assertTrue(config.contains("quarkus.native.resources.includes=publicKey.pem,privateKey.pem"));

        // now run it again, it should complain but leave the files alone
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key");
        Assertions.assertTrue(Files.exists(privateKey));
        Assertions.assertTrue(Files.exists(publicKey));
        List<String> privateKeyContents2 = Files.readAllLines(privateKey);
        Assertions.assertEquals(privateKeyContents, privateKeyContents2);
        List<String> publicKeyContents2 = Files.readAllLines(publicKey);
        Assertions.assertEquals(publicKeyContents, publicKeyContents2);
        Assertions.assertTrue(result.stdout.contains("Public and private keys already exist in"));

        // now run it again in force, to override the files
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--force");
        Assertions.assertTrue(Files.exists(privateKey));
        Assertions.assertTrue(Files.exists(publicKey));
        privateKeyContents2 = Files.readAllLines(privateKey);
        Assertions.assertNotEquals(privateKeyContents, privateKeyContents2);
        publicKeyContents2 = Files.readAllLines(publicKey);
        Assertions.assertNotEquals(publicKeyContents, publicKeyContents2);
        Assertions.assertTrue(result.stdout.contains("Public and private keys created in"));

        // run it for 4096
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--force", "--size", "4096");
        Assertions.assertTrue(Files.exists(privateKey));
        verifyKeyParameters(Files.readAllLines(privateKey), "RSA", 4096);

        // run it for EC
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--force", "--algo", "EC");
        Assertions.assertTrue(Files.exists(privateKey));
        verifyKeyParameters(Files.readAllLines(privateKey), "EC", 256);

        // run it for EC/521
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--force", "--algo", "EC", "--size", "521");
        Assertions.assertTrue(Files.exists(privateKey));
        verifyKeyParameters(Files.readAllLines(privateKey), "EC", 521);

        // verify that quarkus.native.resources.includes appends to existing values
        Files.delete(privateKey);
        Files.delete(publicKey);
        Files.writeString(appProps, "quarkus.native.resources.includes=myResource.txt\n");
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--force");
        config = Files.readString(appProps);
        Assertions.assertTrue(config.contains("quarkus.native.resources.includes=myResource.txt,publicKey.pem,privateKey.pem"),
                "Should append to existing includes, got: " + config);

        // verify that existing config pointing to different keys prevents generation
        Files.delete(privateKey);
        Files.delete(publicKey);
        Files.writeString(appProps, "smallrye.jwt.sign.key.location=other-key.pem\n");
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key");
        Assertions.assertFalse(Files.exists(privateKey), "Should not generate keys when config points to different files");
        Assertions.assertTrue(result.stdout.contains("already points to"),
                "Should warn about conflicting config, got: " + result.stdout);

        // verify that --force overrides the config check
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--force");
        Assertions.assertTrue(Files.exists(privateKey), "Should generate keys with --force despite conflicting config");
    }

    @Test
    public void testCommandEncryption() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--encryption");
        Assertions.assertTrue(Files.exists(privateKey));
        Assertions.assertTrue(Files.exists(publicKey));
        Assertions.assertTrue(result.stdout.contains("Public and private keys created in"));
        String config = Files.readString(appProps);
        Assertions.assertTrue(config.contains("mp.jwt.decrypt.key.location=privateKey.pem"));
        Assertions.assertTrue(config.contains("smallrye.jwt.encrypt.key.location=publicKey.pem"));
        Assertions.assertFalse(config.contains("mp.jwt.verify.publickey.location"));
        Assertions.assertFalse(config.contains("smallrye.jwt.sign.key.location"));
        Assertions.assertTrue(config.contains("quarkus.native.resources.includes=publicKey.pem,privateKey.pem"));

        // verify that a decryption key location pointing to a different key prevents generation only with --enc
        Files.delete(privateKey);
        Files.delete(publicKey);
        Files.writeString(appProps, "mp.jwt.decrypt.key.location=other-key.pem\n");
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--enc");
        Assertions.assertFalse(Files.exists(privateKey), "Should not generate keys when config points to different files");
        Assertions.assertTrue(result.stdout.contains("already points to"),
                "Should warn about conflicting config, got: " + result.stdout);
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key");
        Assertions.assertTrue(Files.exists(privateKey), "Should ignore the decryption key location without --enc");
    }

    @Test
    public void testCommandEncryptionWithExistingKeys() throws Exception {
        CliDriver.execute(workspaceRoot, "jwt", "generate-key");
        List<String> privateKeyContents = Files.readAllLines(privateKey);
        String config = Files.readString(appProps);

        // now run it again with --encryption, it should not configure the signing keys for encryption
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--encryption");
        Assertions.assertTrue(result.stdout.contains("Public and private keys already exist in"));
        Assertions.assertEquals(privateKeyContents, Files.readAllLines(privateKey));
        Assertions.assertEquals(config, Files.readString(appProps));
    }

    @Test
    public void testCommandSeparateKeyPairs() throws Exception {
        CliDriver.execute(workspaceRoot, "jwt", "generate-key");
        List<String> privateKeyContents = Files.readAllLines(privateKey);

        // a second run with other file names adds an encryption key pair, without --force
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--encryption",
                "--private-key-file", "encPrivateKey.pem", "--public-key-file", "encPublicKey.pem");
        Assertions.assertTrue(result.stdout.contains("Public and private keys created in"),
                "Should generate the encryption keys, got: " + result.stdout);
        Assertions.assertTrue(Files.exists(encPrivateKey));
        Assertions.assertTrue(Files.exists(encPublicKey));
        Assertions.assertEquals(privateKeyContents, Files.readAllLines(privateKey), "Should leave the signing keys alone");
        Assertions.assertNotEquals(privateKeyContents, Files.readAllLines(encPrivateKey));
        String config = Files.readString(appProps);
        Assertions.assertTrue(config.contains("mp.jwt.verify.publickey.location=publicKey.pem"));
        Assertions.assertTrue(config.contains("smallrye.jwt.sign.key.location=privateKey.pem"));
        Assertions.assertTrue(config.contains("mp.jwt.decrypt.key.location=encPrivateKey.pem"));
        Assertions.assertTrue(config.contains("smallrye.jwt.encrypt.key.location=encPublicKey.pem"));
        Assertions.assertTrue(config.contains(
                "quarkus.native.resources.includes=publicKey.pem,privateKey.pem,encPublicKey.pem,encPrivateKey.pem"),
                "Should list all four key files, got: " + config);

        // the conflict check compares against the chosen file names
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--encryption");
        Assertions.assertTrue(result.stdout.contains("already points to"),
                "Should warn about conflicting config, got: " + result.stdout);
        Assertions.assertEquals(config, Files.readString(appProps));
    }

    @Test
    public void testCommandKeyFilesInSubdirectory() throws Exception {
        // the private key name is not normalized on purpose: the configuration must still get keys/privateKey.pem
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "jwt", "generate-key",
                "--private-key-file", "./keys/../keys/privateKey.pem", "--public-key-file", "keys/publicKey.pem");
        Assertions.assertTrue(result.stdout.contains("Public and private keys created in"),
                "Should create the subdirectory and the keys, got: " + result.stdout);
        Assertions.assertTrue(Files.exists(keysDir.resolve("privateKey.pem")));
        Assertions.assertTrue(Files.exists(keysDir.resolve("publicKey.pem")));
        Assertions.assertFalse(Files.exists(privateKey));
        String config = Files.readString(appProps);
        Assertions.assertTrue(config.contains("mp.jwt.verify.publickey.location=keys/publicKey.pem"));
        Assertions.assertTrue(config.contains("smallrye.jwt.sign.key.location=keys/privateKey.pem"));
        Assertions.assertTrue(config.contains("quarkus.native.resources.includes=keys/publicKey.pem,keys/privateKey.pem"));
    }

    @Test
    public void testCommandRejectsInvalidKeyFileNames() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "jwt", "generate-key",
                "--private-key-file", "key.pem", "--public-key-file", "key.pem");
        Assertions.assertTrue(result.stdout.contains("must name different files"),
                "Should reject identical key file names, got: " + result.stdout);
        Assertions.assertFalse(Files.exists(workspaceRoot.resolve("src/main/resources/key.pem")));
        Assertions.assertFalse(Files.exists(appProps));

        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key", "--private-key-file", "../escaped.pem");
        Assertions.assertTrue(result.stdout.contains("inside src/main/resources"),
                "Should reject a file name outside src/main/resources, got: " + result.stdout);
        Assertions.assertFalse(Files.exists(workspaceRoot.resolve("src/main/escaped.pem")));
        Assertions.assertFalse(Files.exists(appProps));

        // only one of the two default files exists: refuse instead of claiming both already exist
        Files.createDirectories(publicKey.getParent());
        Files.writeString(publicKey, "stale\n");
        result = CliDriver.execute(workspaceRoot, "jwt", "generate-key");
        Assertions.assertTrue(result.stdout.contains("already exists but its counterpart does not"),
                "Should report the lone existing key file, got: " + result.stdout);
        Assertions.assertFalse(Files.exists(privateKey));
        Assertions.assertFalse(Files.exists(appProps));
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
                Assertions.assertTrue(observedSize > keySize - 8 && observedSize <= keySize,
                        "RSA key size is not valid: " + observedSize + " (expected " + keySize + ")");
            } else {
                observedSize = ((ECKey) privateKey).getParams().getCurve().getField().getFieldSize();
                Assertions.assertEquals(keySize, observedSize,
                        "EC key field size is not valid: " + observedSize + " (expected " + keySize + ")");
            }
        } catch (InvalidKeySpecException e) {
            Assertions.fail("Key is not of the expected algo: " + algo, e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

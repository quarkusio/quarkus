package io.quarkus.cli.config;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Callable;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "encrypt", aliases = "enc", header = "Encrypt Secrets using AES/GCM/NoPadding algorithm by default")
public class Encrypt extends BaseConfigCommand implements Callable<Integer> {
    @Option(required = true, names = { "-s", "--secret" }, description = "Secret")
    String secret;

    @Option(names = { "-k", "--key" }, description = "Encryption Key")
    String encryptionKey;

    @Option(names = { "-f", "--format" }, description = "Encryption Key Format (base64 / plain)", defaultValue = "base64")
    KeyFormat encryptionKeyFormat;

    @Option(hidden = true, names = { "-a", "--algorithm" }, description = "Algorithm", defaultValue = "AES")
    String algorithm;

    @Option(hidden = true, names = { "-m", "--mode" }, description = "Mode", defaultValue = "GCM")
    String mode;

    @Option(hidden = true, names = { "-p", "--padding" }, description = "Algorithm", defaultValue = "NoPadding")
    String padding;

    @Option(hidden = true, names = { "-q", "--quiet" }, defaultValue = "false")
    boolean quiet;

    private String encryptedSecret;

    @Override
    public Integer call() throws Exception {
        if (encryptionKey == null) {
            encryptionKey = encodeToString(generateEncryptionKey().getEncoded());
        } else {
            if (encryptionKeyFormat.equals(KeyFormat.base64)) {
                encryptionKey = encodeToString(encryptionKey.getBytes());
            }
        }

        Cipher cipher = Cipher.getInstance(algorithm + "/" + mode + "/" + padding);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(encryptionKey.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha256.digest(), "AES"), new GCMParameterSpec(128, iv));

        byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));

        ByteBuffer message = ByteBuffer.allocate(1 + iv.length + encrypted.length);
        message.put((byte) iv.length);
        message.put(iv);
        message.put(encrypted);

        this.encryptedSecret = Base64.getUrlEncoder().withoutPadding().encodeToString((message.array()));
        if (!quiet) {
            System.out.println("Encrypted Secret: " + encryptedSecret);
            System.out.println("Encryption Key: " + encryptionKey);
        }

        return 0;
    }

    private SecretKey generateEncryptionKey() {
        try {
            return KeyGenerator.getInstance(algorithm).generateKey();
        } catch (Exception e) {
            System.err.println("Error while generating the encryption key: " + e);
            System.exit(-1);
        }
        return null;
    }

    public String getEncryptedSecret() {
        return encryptedSecret;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public enum KeyFormat {
        base64,
        plain
    }
}

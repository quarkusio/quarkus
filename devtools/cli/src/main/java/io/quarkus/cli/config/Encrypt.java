package io.quarkus.cli.config;

import static io.quarkus.devtools.messagewriter.MessageIcons.SUCCESS_ICON;

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
import picocli.CommandLine.Parameters;

@Command(name = "encrypt", aliases = "enc", header = "Encrypt Secrets", description = "Encrypt a Secret value using the AES/GCM/NoPadding algorithm as a default. The encryption key is generated unless a specific key is set with the --key option.")
public class Encrypt extends BaseConfigCommand implements Callable<Integer> {
    @Parameters(index = "0", paramLabel = "SECRET", description = "The Secret value to encrypt")
    String secret;

    @Option(names = { "-k", "--key" }, description = "The Encryption Key")
    String encryptionKey;

    @Option(names = { "-f", "--format" }, description = "The Encryption Key Format (base64 / plain)", defaultValue = "base64")
    KeyFormat encryptionKeyFormat;

    @Option(hidden = true, names = { "-a", "--algorithm" }, description = "Algorithm", defaultValue = "AES")
    String algorithm;

    @Option(hidden = true, names = { "-m", "--mode" }, description = "Mode", defaultValue = "GCM")
    String mode;

    @Option(hidden = true, names = { "-p", "--padding" }, description = "Padding", defaultValue = "NoPadding")
    String padding;

    @Option(hidden = true, names = { "-q", "--quiet" }, defaultValue = "false")
    boolean quiet;

    private String encryptedSecret;

    @Override
    public Integer call() throws Exception {
        boolean generatedKey = false;
        if (encryptionKey == null) {
            encryptionKey = encodeToString(generateEncryptionKey().getEncoded());
            generatedKey = true;
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
            String success = SUCCESS_ICON + " The secret @|bold " + secret + "|@ was encrypted to @|bold " + encryptedSecret
                    + "|@";
            if (generatedKey) {
                success = success + " with the generated encryption key (" + encryptionKeyFormat + "): @|bold " + encryptionKey
                        + "|@";
            }
            output.info(success);
        }

        return 0;
    }

    private SecretKey generateEncryptionKey() {
        try {
            return KeyGenerator.getInstance(algorithm).generateKey();
        } catch (Exception e) {
            output.error("Error while generating the encryption key: ");
            output.printStackTrace(e);
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

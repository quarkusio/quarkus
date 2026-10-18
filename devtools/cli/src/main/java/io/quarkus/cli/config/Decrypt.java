package io.quarkus.cli.config;

import static io.quarkus.devtools.messagewriter.MessageIcons.SUCCESS_ICON;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.Callable;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.quarkus.cli.config.Encrypt.KeyFormat;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "decrypt", aliases = "dec", header = "Decrypt Secrets", description = "Decrypt a Secret value using the AES/GCM/NoPadding algorithm as a default.")
public class Decrypt extends BaseConfigCommand implements Callable<Integer> {
    @Parameters(index = "0", paramLabel = "SECRET", description = "The secret value to decrypt")
    String secret;

    @Parameters(index = "1", paramLabel = "DECRYPTION KEY", description = "The decryption key")
    String decryptionKey;

    @Option(names = { "-f", "--format" }, description = "The decryption key format (base64 / plain)", defaultValue = "base64")
    KeyFormat decryptionKeyFormat;

    @Option(hidden = true, names = { "-a", "--algorithm" }, description = "Algorithm", defaultValue = "AES")
    String algorithm;

    @Option(hidden = true, names = { "-m", "--mode" }, description = "Mode", defaultValue = "GCM")
    String mode;

    @Option(hidden = true, names = { "-p", "--padding" }, description = "Padding", defaultValue = "NoPadding")
    String padding;

    @Option(hidden = true, names = { "-q", "--quiet" }, defaultValue = "false")
    boolean quiet;

    @Override
    public Integer call() throws Exception {
        if (decryptionKey.startsWith("\\\"") && decryptionKey.endsWith("\"\\")) {
            decryptionKey = decryptionKey.substring(2, decryptionKey.length() - 2);
        }

        byte[] decryptionKeyBytes;
        if (decryptionKeyFormat.equals(KeyFormat.base64)) {
            decryptionKeyBytes = Base64.getUrlDecoder().decode(decryptionKey);
        } else {
            decryptionKeyBytes = decryptionKey.getBytes(UTF_8);
        }

        Cipher cipher = Cipher.getInstance(algorithm + "/" + mode + "/" + padding);
        ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode(secret.getBytes(UTF_8)));
        int ivLength = byteBuffer.get();
        byte[] iv = new byte[ivLength];
        byteBuffer.get(iv);
        byte[] encrypted = new byte[byteBuffer.remaining()];
        byteBuffer.get(encrypted);

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(decryptionKeyBytes);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha256.digest(), "AES"), new GCMParameterSpec(128, iv));
        String decrypted = new String(cipher.doFinal(encrypted), UTF_8);

        if (!quiet) {
            String success = SUCCESS_ICON + " The secret @|bold " + secret + "|@ was decrypted to @|bold " + decrypted + "|@";
            output.info(success);
        }

        return 0;
    }
}

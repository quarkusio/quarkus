package io.quarkus.tls.runtime.config;

import java.nio.file.Path;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import javax.crypto.EncryptedPrivateKeyInfo;

/**
 * Explains why an encrypted PEM (PKCS#8) key could not be decrypted. The parser only reports that decryption failed,
 * so the likely cause is derived from the key file itself and from the installed security providers.
 */
final class EncryptedPemDiagnostics {

    private static final String HEADER = "-----BEGIN ENCRYPTED PRIVATE KEY-----";
    private static final String FOOTER = "-----END ENCRYPTED PRIVATE KEY-----";

    private EncryptedPemDiagnostics() {
    }

    static String explain(Path key, String content) {
        String prefix = "Unable to decrypt the key file " + key;
        try {
            int start = content.indexOf(HEADER);
            if (start < 0) {
                return prefix + ": it is not an encrypted PKCS#8 key (there is no '" + HEADER + "' header)."
                        + " Remove the password property for a key that is not encrypted, or use an encrypted PKCS#8 key";
            }
            int end = content.indexOf(FOOTER, start);
            String base64 = content.substring(start + HEADER.length(), end < 0 ? content.length() : end)
                    .replaceAll("\\s", "");
            String algorithm = new EncryptedPrivateKeyInfo(Base64.getDecoder().decode(base64)).getAlgName();
            List<String> fipsProviders = fipsProviders();
            if (!fipsProviders.isEmpty()) {
                return prefix + ": the key is encrypted with '" + algorithm
                        + "', which is generally not available from FIPS providers (" + String.join(", ", fipsProviders)
                        + "). Use a PKCS12 key store instead";
            }
            return prefix + ": wrong password, or the encryption algorithm '" + algorithm
                    + "' is not supported by the installed security providers";
        } catch (Exception e) {
            return prefix + ": the encrypted key is malformed";
        }
    }

    private static List<String> fipsProviders() {
        List<String> names = new ArrayList<>();
        for (Provider provider : Security.getProviders()) {
            if (provider.getName().toUpperCase(Locale.ROOT).contains("FIPS")) {
                names.add(provider.getName());
            }
        }
        return names;
    }
}

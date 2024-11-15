package io.quarkus.tls.runtime.config;

import static io.quarkus.tls.runtime.config.TlsConfigUtils.read;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.certs.pem.parsers.EncryptedPKCS8Parser;
import io.smallrye.config.WithParentName;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemKeyCertOptions;

@ConfigGroup
public interface PemKeyCertConfig {

    /**
     * List of the PEM key/cert files (Pem format).
     */
    @WithParentName
    Map<String, KeyCertConfig> keyCerts();

    /**
     * The order of the key/cert files, based on the names in the `keyCerts` map.
     * <p>
     * By default, Quarkus sorts the key using a lexicographical order.
     * This property allows you to specify the order of the key/cert files.
     */
    Optional<List<String>> order();

    default PemKeyCertOptions toOptions() {
        PemKeyCertOptions options = new PemKeyCertOptions();

        if (keyCerts().isEmpty()) {
            throw new IllegalArgumentException("You must specify the key files and certificate files");
        }

        List<KeyCertConfig> orderedListOfPair = new ArrayList<>();
        if (order().isPresent()) {
            // Check the size of the order list. It must match the size of the keyCerts map.
            if (order().get().size() != keyCerts().size()) {
                throw new IllegalArgumentException("The size of the `order` list (" + order().get().size() + ") must " +
                        "match the size of the `keyCerts` map (" + keyCerts().size() + ")");
            }

            // We use the order specified by the user.
            for (String name : order().get()) {
                KeyCertConfig keyCert = keyCerts().get(name);
                if (keyCert == null) {
                    throw new IllegalArgumentException("The key/cert pair with the name '" + name
                            + "' is not found in the `order` list: " + order().get());
                }
                orderedListOfPair.add(keyCert);
            }
        } else {
            // Use the lexical order.
            orderedListOfPair.addAll(new TreeMap<>(keyCerts()).values());
        }

        for (KeyCertConfig config : orderedListOfPair) {
            options.addCertValue(Buffer.buffer(read(config.cert())));
            if (config.password().isPresent()) {
                byte[] content = read(config.key());
                String contentAsString = new String(content, StandardCharsets.UTF_8);
                Buffer decrypted = new EncryptedPKCS8Parser().decryptKey(contentAsString, config.password().get());
                if (decrypted == null) {
                    throw new IllegalArgumentException("Unable to decrypt the key file: " + config.key());
                }
                options.addKeyValue(decrypted);
            } else {
                options.addKeyValue(Buffer.buffer(read(config.key())));
            }
        }
        return options;
    }

    interface KeyCertConfig {

        /**
         * The path to the key file (in PEM format: PKCS#8, PKCS#1 or encrypted PKCS#8).
         */
        Path key();

        /**
         * The path to the certificate file (in PEM format).
         */
        Path cert();

        /**
         * When the key is encrypted (encrypted PKCS#8), the password to decrypt it.
         */
        Optional<String> password();
    }

}

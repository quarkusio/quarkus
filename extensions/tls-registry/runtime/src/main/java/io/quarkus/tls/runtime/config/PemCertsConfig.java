package io.quarkus.tls.runtime.config;

import static io.quarkus.tls.runtime.config.TlsConfigUtils.read;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;

@ConfigGroup
public interface PemCertsConfig {

    /**
     * List of the trusted cert paths (Pem format).
     */
    Optional<List<Path>> certs();

    default PemTrustOptions toOptions() {
        PemTrustOptions options = new PemTrustOptions();

        if (certs().isEmpty()) {
            throw new IllegalArgumentException("You must specify the key files and certificate files");
        }
        for (Path path : certs().get()) {
            options.addCertValue(Buffer.buffer(read(path)));
        }
        return options;
    }

    interface KeyCertConfig {

        /**
         * The path to the key file (in PEM format).
         */
        Path key();

        /**
         * The path to the certificate file (in PEM format).
         */
        Path cert();
    }

}

package io.quarkus.tls.runtime.config;

import static io.quarkus.tls.runtime.config.TlsConfigUtils.read;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;

@ConfigGroup
public interface PemCertsConfig {

    /**
     * List of the trusted cert paths (Pem format).
     */
    Optional<List<Path>> certs();

    /**
     * List of the directories with the trusted certificates (Pem format). The configured directory can be empty.
     * Any file in the configured directories will be treated as a trusted certificate in the Pem format.
     */
    Optional<List<Path>> certDirs();

    /**
     * List of the trusted certificates passed inline, each value being the content of a PEM file encoded in base64.
     * Use it to pass the certificates (for example, from an environment variable) instead of reading them from files.
     * The certificates are added to the ones configured with {@code certs} and {@code cert-dirs}.
     */
    Optional<List<String>> certValues();

    default boolean hasNoTrustedCertificates() {
        if (certs().isPresent() && !certs().get().isEmpty()) {
            return false;
        }

        if (certValues().isPresent() && !certValues().get().isEmpty()) {
            return false;
        }

        List<Path> certDirs = certDirs().orElse(null);
        if (certDirs != null && !certDirs.isEmpty()) {
            // whether any of certificate directories contains at least one file
            for (Path certDir : certDirs) {
                try (var ds = streamDirectory(certDir)) {
                    if (ds.iterator().hasNext()) {
                        return false;
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to close directory stream opened for certificate directory " + certDir,
                            e);
                }
            }
            var logger = Logger.getLogger(PemCertsConfig.class);
            if (logger.isDebugEnabled()) {
                logger.debugf("There is %d configured directories for the trusted certificates (%s), but "
                        + "none of the directories contains any file", certDirs.size(), certDirs);
            }
        }

        return true;
    }

    default PemTrustOptions toOptions() {
        PemTrustOptions options = new PemTrustOptions();

        var certs = certs().orElse(null);
        if (certs != null) {
            for (Path path : certs) {
                options.addCertValue(Buffer.buffer(read(path)));
            }
        }

        List<Path> certDirs = certDirs().orElse(null);
        if (certDirs != null) {
            for (Path certDir : certDirs) {
                try (var ds = streamDirectory(certDir)) {
                    for (Path cert : ds) {
                        options.addCertValue(Buffer.buffer(read(cert)));
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to close directory stream opened for certificate directory " + certDir,
                            e);
                }
            }
        }

        var certValues = certValues().orElse(null);
        if (certValues != null) {
            for (String value : certValues) {
                try {
                    options.addCertValue(Buffer.buffer(TlsConfigUtils.decodeBase64(value)));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("A value of the `cert-values` property is not a valid base64 string", e);
                }
            }
        }

        if (options.getCertValues().isEmpty()) {
            throw new IllegalArgumentException("You must specify the key files and certificate files");
        }

        return options;
    }

    private static DirectoryStream<Path> streamDirectory(Path certificateDirectory) {
        if (Files.notExists(certificateDirectory)) {
            throw new ConfigurationException("Configured certificate path does not exist:" + certificateDirectory);
        }

        if (!Files.isDirectory(certificateDirectory)) {
            throw new ConfigurationException("Path '" + certificateDirectory + "' is not a directory. Paths pointing "
                    + "to the certificate files can be configured with the 'quarkus.tls.trust-store.pem.certs' property"
                    + " instead");
        }

        try {
            return Files.newDirectoryStream(certificateDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open DirectoryStream for configured certificate path " + certificateDirectory,
                    e);
        }
    }
}

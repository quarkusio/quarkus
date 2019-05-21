package io.quarkus.runtime.configuration.ssl;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * A certificate configuration. Either the certificate and key files must be given, or a key store must be given.
 */
@ConfigGroup
public class CertificateConfig {
    /**
     * The file path to a server certificate or certificate chain in PEM format.
     */
    @ConfigItem
    public Optional<Path> file;

    /**
     * The file path to the corresponding certificate private key file in PEM format.
     */
    @ConfigItem
    public Optional<Path> keyFile;

    /**
     * An optional key store which holds the certificate information instead of specifying separate files.
     */
    @ConfigItem
    public Optional<Path> keyStoreFile;

    /**
     * An optional parameter to specify type of the key store file. If not given, the type is automatically detected
     * based on the file name.
     */
    @ConfigItem
    public Optional<String> keyStoreFileType;

    /**
     * A parameter to specify the password of the key store file. If not given, the default ("password") is used.
     */
    @ConfigItem(defaultValue = "password")
    public String keyStorePassword;
}

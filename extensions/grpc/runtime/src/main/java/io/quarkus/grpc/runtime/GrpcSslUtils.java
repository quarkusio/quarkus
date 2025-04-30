package io.quarkus.grpc.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.runtime.util.ClassPathUtils;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;

@SuppressWarnings("OptionalIsPresent")
public class GrpcSslUtils {

    private static final Logger LOGGER = Logger.getLogger(GrpcSslUtils.class.getName());

    /**
     * Get an {@code HttpServerOptions} for this server configuration, or null if SSL should not be enabled.
     *
     * @return whether plain text is used.
     */
    static boolean applySslOptions(GrpcServerConfiguration config, HttpServerOptions options)
            throws IOException {
        // Disable plain-text if the ssl configuration is set.
        if (config.plainText() && (config.ssl().certificate().isPresent() || config.ssl().keyStore().isPresent())) {
            LOGGER.info("Disabling gRPC plain-text as the SSL certificate is configured");
        }
        if (config.isPlainTextEnabled()) {
            options.setSsl(false);
            return true;
        } else {
            options.setSsl(true);
        }

        GrpcServerConfiguration.SslServerConfig sslConfig = config.ssl();
        final Optional<Path> certFile = sslConfig.certificate();
        final Optional<Path> keyFile = sslConfig.key();
        final Optional<Path> keyStoreFile = sslConfig.keyStore();
        final Optional<Path> trustStoreFile = sslConfig.trustStore();
        final Optional<String> trustStorePassword = sslConfig.trustStorePassword();

        options.setUseAlpn(config.alpn());
        if (config.alpn()) {
            options.setAlpnVersions(Arrays.asList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
        }

        if (certFile.isPresent() && keyFile.isPresent()) {
            createPemKeyCertOptions(certFile.get(), keyFile.get(), options);
        } else if (keyStoreFile.isPresent()) {
            final Path keyStorePath = keyStoreFile.get();
            final Optional<String> keyStoreFileType = sslConfig.keyStoreType();
            String type;
            if (keyStoreFileType.isPresent()) {
                type = keyStoreFileType.get().toLowerCase();
            } else {
                type = findKeystoreFileType(keyStorePath);
            }

            byte[] data = getFileContent(keyStorePath);
            switch (type) {
                case "pkcs12": {
                    PfxOptions o = new PfxOptions()
                            .setValue(Buffer.buffer(data));
                    if (sslConfig.keyStorePassword().isPresent()) {
                        o.setPassword(sslConfig.keyStorePassword().get());
                    }
                    if (sslConfig.keyStoreAlias().isPresent()) {
                        o.setAlias(sslConfig.keyStoreAlias().get());
                        if (sslConfig.keyStoreAliasPassword().isPresent()) {
                            o.setAliasPassword(sslConfig.keyStoreAliasPassword().get());
                        }
                    }
                    options.setPfxKeyCertOptions(o);
                    break;
                }
                case "jks": {
                    JksOptions o = new JksOptions()
                            .setValue(Buffer.buffer(data));
                    if (sslConfig.keyStorePassword().isPresent()) {
                        o.setPassword(sslConfig.keyStorePassword().get());
                    }
                    if (sslConfig.keyStoreAlias().isPresent()) {
                        o.setAlias(sslConfig.keyStoreAlias().get());
                        if (sslConfig.keyStoreAliasPassword().isPresent()) {
                            o.setAliasPassword(sslConfig.keyStoreAliasPassword().get());
                        }
                    }
                    options.setKeyStoreOptions(o);
                    break;
                }
                default:
                    throw new IllegalArgumentException(
                            "Unknown keystore type: " + type + " valid types are jks or pkcs12");
            }

        }

        if (trustStoreFile.isPresent()) {
            if (trustStorePassword.isEmpty()) {
                throw new IllegalArgumentException("No trust store password provided");
            }
            String type;
            final Optional<String> trustStoreFileType = sslConfig.trustStoreType();
            final Path trustStoreFilePath = trustStoreFile.get();
            if (trustStoreFileType.isPresent()) {
                type = trustStoreFileType.get();
            } else {
                type = findKeystoreFileType(trustStoreFilePath);
            }
            createTrustStoreOptions(trustStoreFilePath, trustStorePassword.get(), type, options);
        }

        for (String cipher : sslConfig.cipherSuites().orElse(Collections.emptyList())) {
            options.addEnabledCipherSuite(cipher);
        }
        options.setEnabledSecureTransportProtocols(sslConfig.protocols());
        options.setClientAuth(sslConfig.clientAuth());
        return false;
    }

    private static byte[] getFileContent(Path path) throws IOException {
        byte[] data;
        final InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(ClassPathUtils.toResourceName(path));
        if (resource != null) {
            try (InputStream is = resource) {
                data = doRead(is);
            }
        } else {
            try (InputStream is = Files.newInputStream(path)) {
                data = doRead(is);
            }
        }
        return data;
    }

    private static void createPemKeyCertOptions(Path certFile, Path keyFile,
            HttpServerOptions serverOptions) throws IOException {
        final byte[] cert = getFileContent(certFile);
        final byte[] key = getFileContent(keyFile);
        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                .setCertValue(Buffer.buffer(cert))
                .setKeyValue(Buffer.buffer(key));
        serverOptions.setPemKeyCertOptions(pemKeyCertOptions);
    }

    private static void createTrustStoreOptions(Path trustStoreFile, String trustStorePassword,
            String trustStoreFileType, HttpServerOptions serverOptions) throws IOException {
        byte[] data = getFileContent(trustStoreFile);
        switch (trustStoreFileType) {
            case "pkcs12": {
                PfxOptions options = new PfxOptions()
                        .setPassword(trustStorePassword)
                        .setValue(Buffer.buffer(data));
                serverOptions.setPfxTrustOptions(options);
                break;
            }
            case "jks": {
                JksOptions options = new JksOptions()
                        .setPassword(trustStorePassword)
                        .setValue(Buffer.buffer(data));
                serverOptions.setTrustStoreOptions(options);
                break;
            }
            default:
                throw new IllegalArgumentException(
                        "Unknown truststore type: " + trustStoreFileType + " valid types are jks or pkcs12");
        }
    }

    private static String findKeystoreFileType(Path storePath) {
        final String pathName = storePath.toString();
        if (pathName.endsWith(".p12") || pathName.endsWith(".pkcs12") || pathName.endsWith(".pfx")) {
            return "pkcs12";
        } else {
            // assume jks
            return "jks";
        }
    }

    private static byte[] doRead(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    private GrpcSslUtils() {
    }
}

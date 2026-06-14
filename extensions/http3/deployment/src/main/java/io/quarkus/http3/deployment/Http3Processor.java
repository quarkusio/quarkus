package io.quarkus.http3.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.http3.deployment.spi.Http3EnabledBuildItem;
import io.quarkus.http3.runtime.CertOrigin;
import io.quarkus.http3.runtime.Http3AltSvcHandler;
import io.quarkus.http3.runtime.Http3Customizer;
import io.quarkus.http3.runtime.Http3DevTlsSupplier;
import io.quarkus.http3.runtime.Http3Recorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.deployment.spi.TlsCertificateBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.spi.HttpServerStartedBuildItem;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;

class Http3Processor {

    private static final Logger LOG = Logger.getLogger(Http3Processor.class);

    private static final String FEATURE = "http3";

    private static final String DEV_TLS_NAME = "http3-dev";
    private static final String DEV_TLS_PASSWORD = "http3-dev-password";

    private static final File DEV_CA_FILE = new File(System.getProperty("user.home"), ".quarkus/quarkus-dev-root-ca.pem");
    private static final File DEV_CA_KEY_FILE = new File(System.getProperty("user.home"),
            ".quarkus/quarkus-dev-root-key.pem");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    Http3EnabledBuildItem enable(Http3BuildTimeConfig config) {
        if (!config.enabled()) {
            return null;
        }

        if (!isQuicNativeAvailable()) {
            throw new ConfigurationException(
                    "HTTP/3 is enabled but the native QUIC library is not on the classpath. " +
                            "Add a dependency on io.netty:netty-codec-native-quic with the classifier " +
                            "matching your platform (e.g. linux-x86_64, osx-aarch_64). " +
                            "Set quarkus.http3.enabled=false to disable HTTP/3.");
        }

        LOG.info("HTTP/3 (QUIC) support enabled");
        return new Http3EnabledBuildItem();
    }

    @BuildStep
    AdditionalBeanBuildItem registerCustomizer(Http3BuildTimeConfig config) {
        if (!config.enabled()) {
            return null;
        }
        return AdditionalBeanBuildItem.unremovableOf(Http3Customizer.class);
    }

    @BuildStep
    FilterBuildItem altSvcFilter(Http3BuildTimeConfig config, Http3EnabledBuildItem http3Enabled) {
        if (!config.altSvc()) {
            return null;
        }
        return new FilterBuildItem(new Http3AltSvcHandler(), 10);
    }

    @BuildStep
    Http3CertOriginBuildItem autoConfigureTls(
            Http3EnabledBuildItem http3Enabled,
            LaunchModeBuildItem launchMode,
            BuildProducer<TlsCertificateBuildItem> tlsCertificateProducer,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfigDefaultProducer) {

        if (isTlsAlreadyConfigured()) {
            return new Http3CertOriginBuildItem(CertOrigin.CONFIGURED);
        }

        if (launchMode.getLaunchMode() == LaunchMode.NORMAL) {
            LOG.warn("HTTP/3 is enabled but no TLS configuration was detected at build time. "
                    + "HTTP/3 requires TLS. If TLS is not configured at runtime, the server will fail to start. "
                    + "Configure quarkus.tls.key-store.* or set quarkus.http3.enabled=false to disable HTTP/3.");
            return new Http3CertOriginBuildItem(CertOrigin.CONFIGURED);
        }

        // Dev or test mode: auto-generate a certificate
        CertOrigin certOrigin;
        Path outputDir = Path.of("target", "http3-dev-cert");
        try {
            Files.createDirectories(outputDir);

            CertificateRequest request = new CertificateRequest()
                    .withName(DEV_TLS_NAME)
                    .withCN("localhost")
                    .withPassword(DEV_TLS_PASSWORD)
                    .withDuration(Duration.ofDays(365))
                    .withFormat(Format.PKCS12);

            if (DEV_CA_FILE.exists() && DEV_CA_KEY_FILE.exists()) {
                X509Certificate caCert = loadDevCaCertificate();
                PrivateKey caKey = loadDevCaPrivateKey();
                request.signedWith(caCert, caKey);
                LOG.info("HTTP/3: auto-generated TLS certificate signed by Quarkus Dev CA");
                certOrigin = CertOrigin.DEV_CA;
            } else {
                LOG.info("HTTP/3: auto-generated self-signed TLS certificate "
                        + "(run 'quarkus tls generate-quarkus-ca --install' for browser-trusted certificates)");
                certOrigin = CertOrigin.SELF_SIGNED;
            }

            new CertificateGenerator(outputDir, true).generate(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to auto-generate TLS certificate for HTTP/3 dev mode", e);
        }

        String keystorePath = outputDir.resolve(DEV_TLS_NAME + "-keystore.p12").toAbsolutePath().toString();
        tlsCertificateProducer.produce(
                new TlsCertificateBuildItem(DEV_TLS_NAME, new Http3DevTlsSupplier(keystorePath, DEV_TLS_PASSWORD)));
        runtimeConfigDefaultProducer.produce(
                new RunTimeConfigurationDefaultBuildItem("quarkus.http.tls-configuration-name", DEV_TLS_NAME));
        runtimeConfigDefaultProducer.produce(
                new RunTimeConfigurationDefaultBuildItem("quarkus.http.insecure-requests", "redirect"));
        LOG.info("HTTP/3: insecure HTTP requests will be redirected to HTTPS "
                + "(set quarkus.http.insecure-requests=enabled to allow plain HTTP)");
        return new Http3CertOriginBuildItem(certOrigin);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setCertOrigin(
            Http3EnabledBuildItem http3Enabled,
            Http3CertOriginBuildItem certOriginItem,
            Http3Recorder recorder) {
        recorder.setCertOrigin(certOriginItem.getCertOrigin());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(ServiceStartBuildItem.class)
    void verifyTlsInProductionMode(
            HttpServerStartedBuildItem httpServerStartedBuildItem, // Barrier.
            Http3EnabledBuildItem http3Enabled, // Only produced if configured.
            LaunchModeBuildItem launchMode,
            Http3Recorder recorder) {
        if (launchMode.getLaunchMode() == LaunchMode.NORMAL) {
            recorder.checkTls();
        }
    }

    private static boolean isTlsAlreadyConfigured() {
        var config = ConfigProvider.getConfig();

        Optional<String> tlsConfigName = config.getOptionalValue("quarkus.http.tls-configuration-name", String.class);
        if (tlsConfigName.isPresent()) {
            return true;
        }

        List<String> tlsKeyStoreProperties = List.of(
                "quarkus.tls.key-store.p12.path",
                "quarkus.tls.key-store.jks.path",
                "quarkus.tls.key-store.pem.keys");
        for (String prop : tlsKeyStoreProperties) {
            if (config.getOptionalValue(prop, String.class).isPresent()) {
                return true;
            }
        }

        List<String> legacySslProperties = List.of(
                "quarkus.http.ssl.certificate.key-store-file",
                "quarkus.http.ssl.certificate.files",
                "quarkus.http.ssl.certificate.key-files");
        for (String prop : legacySslProperties) {
            if (config.getOptionalValue(prop, String.class).isPresent()) {
                return true;
            }
        }

        return false;
    }

    private static X509Certificate loadDevCaCertificate() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream fis = new FileInputStream(DEV_CA_FILE)) {
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }

    private static PrivateKey loadDevCaPrivateKey() throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(DEV_CA_KEY_FILE));
                PEMParser pemParser = new PEMParser(reader)) {
            Object obj = pemParser.readObject();
            if (obj instanceof KeyPair kp) {
                return kp.getPrivate();
            } else if (obj instanceof PrivateKeyInfo pki) {
                return new JcaPEMKeyConverter().getPrivateKey(pki);
            } else {
                throw new IllegalStateException(
                        "The file " + DEV_CA_KEY_FILE.getAbsolutePath()
                                + " does not contain a valid private key: " + obj.getClass().getName());
            }
        }
    }

    private static boolean isQuicNativeAvailable() {
        return QuarkusClassLoader.isClassPresentAtRuntime("io.netty.handler.codec.quic.Quiche");
    }

}

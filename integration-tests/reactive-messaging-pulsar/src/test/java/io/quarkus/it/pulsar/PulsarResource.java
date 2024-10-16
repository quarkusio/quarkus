package io.quarkus.it.pulsar;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class PulsarResource implements QuarkusTestResourceLifecycleManager {

    private PulsarContainer container;

    private boolean pem = false;
    private boolean jks = false;
    private String keyStorePassword;
    private String trustStorePassword;
    private String tlsConfigName;

    @Override
    public void init(Map<String, String> initArgs) {
        pem = Boolean.parseBoolean(initArgs.get("isPem"));
        jks = Boolean.parseBoolean(initArgs.get("isJks"));
        keyStorePassword = initArgs.get("keyStorePassword");
        trustStorePassword = initArgs.get("trustStorePassword");
        tlsConfigName = initArgs.get("pulsar.tls-configuration-name");
    }

    @Override
    public Map<String, String> start() {
        container = new PulsarContainer();

        if (pem) {
            configurePem();
        } else if (jks) {
            configureJks();
        }

        container.start();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("pulsar.client.serviceUrl", container.getPulsarBrokerUrl());
        if (tlsConfigName != null) {
            cfg.put("mp.messaging.connector.smallrye-pulsar.tls-configuration-name", tlsConfigName);
        }

        return cfg;
    }

    private void configureJks() {
        configureCommonTls();
        container
                .useTls()
                .withCopyFileToContainer(MountableFile.forHostPath("target/certs/pulsar-keystore.jks"),
                        "/pulsar/conf/pulsar-keystore.jks")
                .withCopyFileToContainer(MountableFile.forHostPath("target/certs/pulsar-server-truststore.jks"),
                        "/pulsar/conf/pulsar-server-truststore.jks")
                // broker client
                .withCopyFileToContainer(MountableFile.forHostPath("target/certs/pulsar-client-keystore.jks"),
                        "/pulsar/conf/pulsar-client-keystore.jks")
                .withCopyFileToContainer(MountableFile.forHostPath("target/certs/pulsar-client-truststore.jks"),
                        "/pulsar/conf/pulsar-client-truststore.jks");
        addConf("tlsEnabledWithKeyStore", "true");
        addConf("tlsKeyStoreType", "JKS");
        addConf("tlsKeyStore", "/pulsar/conf/pulsar-keystore.jks");
        addConf("tlsKeyStorePassword", keyStorePassword);
        addConf("tlsTrustStoreType", "JKS");
        addConf("tlsTrustStore", "/pulsar/conf/pulsar-server-truststore.jks");
        addConf("tlsTrustStorePassword", trustStorePassword);
        // broker client
        addConf("brokerClientTlsEnabledWithKeyStore", "true");
        addConf("brokerClientTlsTrustStoreType", "JKS");
        addConf("brokerClientTlsTrustStore", "/pulsar/conf/pulsar-client-truststore.jks");
        addConf("brokerClientTlsTrustStorePassword", trustStorePassword);
        addConf("brokerClientTlsKeyStoreType", "JKS");
        addConf("brokerClientTlsKeyStore", "/pulsar/conf/pulsar-client-keystore.jks");
        addConf("brokerClientTlsKeyStorePassword", keyStorePassword);
    }

    private void configurePem() {
        configureCommonTls();
        container
                .useTls()
                .withCopyFileToContainer(MountableFile.forHostPath("target/certs/pulsar.crt"), "/pulsar/conf/pulsar.crt")
                .withCopyFileToContainer(MountableFile.forHostPath("target/certs/pulsar.key"), "/pulsar/conf/pulsar.key")
                .withCopyFileToContainer(MountableFile.forHostPath("target/certs/pulsar-server-ca.crt"),
                        "/pulsar/conf/pulsar-server-ca.crt")
                // broker client
                .withCopyFileToContainer(MountableFile.forHostPath("target/certs/pulsar-client.crt"),
                        "/pulsar/conf/pulsar-client.crt")
                .withCopyFileToContainer(MountableFile.forHostPath("target/certs/pulsar-client.key"),
                        "/pulsar/conf/pulsar-client.key")
                .withCopyFileToContainer(MountableFile.forHostPath("target/certs/pulsar-client-ca.crt"),
                        "/pulsar/conf/pulsar-client-ca.crt");
        addConf("tlsRequireTrustedClientCertOnConnect", "true");
        addConf("tlsTrustCertsFilePath", "/pulsar/conf/pulsar-server-ca.crt");
        addConf("tlsCertificateFilePath", "/pulsar/conf/pulsar.crt");
        addConf("tlsKeyFilePath", "/pulsar/conf/pulsar.key");
        // broker client
        addConf("brokerClientTrustCertsFilePath", "/pulsar/conf/pulsar-client-ca.crt");
        addConf("brokerClientCertificateFilePath", "/pulsar/conf/pulsar-client.crt");
        addConf("brokerClientKeyFilePath", "/pulsar/conf/pulsar-client.key");
    }

    private void addConf(String key, String value) {
        container.addEnv("PULSAR_PREFIX_" + key, value);
    }

    private void configureCommonTls() {
        addConf("brokerServicePort", "");
        addConf("brokerServicePortTls", "6651");
        addConf("webServicePortTls", "8443");
        addConf("tlsEnabled", "true");
        addConf("brokerClientTlsEnabled", "true");
    }

    @Override
    public void stop() {
        if (container != null) {
            container.close();
        }
    }
}

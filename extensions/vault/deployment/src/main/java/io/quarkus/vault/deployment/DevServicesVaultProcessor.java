package io.quarkus.vault.deployment;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.vault.runtime.VaultVersions;
import io.quarkus.vault.runtime.config.DevServicesConfig;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;

public class DevServicesVaultProcessor {
    private static final Logger log = Logger.getLogger(DevServicesVaultProcessor.class);
    private static final String VAULT_IMAGE = "vault:" + VaultVersions.VAULT_TEST_VERSION;
    private static final int VAULT_EXPOSED_PORT = 8200;
    private static final String CONFIG_PREFIX = "quarkus.vault.";
    private static final String URL_CONFIG_KEY = CONFIG_PREFIX + "url";
    private static final String AUTH_CONFIG_PREFIX = CONFIG_PREFIX + "authentication.";
    private static final String CLIENT_TOKEN_CONFIG_KEY = AUTH_CONFIG_PREFIX + "client-token";
    private static volatile List<Closeable> closeables;
    private static volatile DevServicesConfig capturedDevServicesConfiguration;
    private static volatile boolean first = true;
    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class)
    public void startVaultContainers(BuildProducer<DevServicesConfigResultBuildItem> devConfig, VaultBuildTimeConfig config) {

        DevServicesConfig currentDevServicesConfiguration = config.devservices;

        // figure out if we need to shut down and restart any existing Vault container
        // if not and the Vault container have already started we just return
        if (closeables != null) {
            boolean restartRequired = !currentDevServicesConfiguration.equals(capturedDevServicesConfiguration);
            if (!restartRequired) {
                return;
            }
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (Throwable e) {
                    log.error("Failed to stop Vault container", e);
                }
            }
            closeables = null;
            capturedDevServicesConfiguration = null;
        }

        capturedDevServicesConfiguration = currentDevServicesConfiguration;

        StartResult startResult = startContainer(currentDevServicesConfiguration);
        if (startResult == null) {
            return;
        }
        Map<String, String> connectionProperties = new HashMap<>();
        connectionProperties.put(URL_CONFIG_KEY, startResult.url);
        connectionProperties.put(CLIENT_TOKEN_CONFIG_KEY, startResult.clientToken);

        closeables = Collections.singletonList(startResult.closeable);

        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (closeables != null) {
                        for (Closeable closeable : closeables) {
                            try {
                                closeable.close();
                            } catch (Throwable t) {
                                log.error("Failed to stop Vault container", t);
                            }
                        }
                    }
                    first = true;
                    closeables = null;
                    capturedDevServicesConfiguration = null;
                }
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
            Thread closeHookThread = new Thread(closeTask, "Vault container shutdown thread");
            Runtime.getRuntime().addShutdownHook(closeHookThread);
            ((QuarkusClassLoader) cl.parent()).addCloseTask(new Runnable() {
                @Override
                public void run() {
                    Runtime.getRuntime().removeShutdownHook(closeHookThread);
                }
            });
        }
        for (Map.Entry<String, String> entry : connectionProperties.entrySet()) {
            devConfig.produce(new DevServicesConfigResultBuildItem(entry.getKey(), entry.getValue()));
        }
    }

    private StartResult startContainer(DevServicesConfig devServicesConfig) {
        if (!devServicesConfig.enabled) {
            // explicitly disabled
            log.debug("Not starting devservices for Vault as it has been disabled in the config");
            return null;
        }

        if (!isDockerWorking.getAsBoolean()) {
            log.warn("Please configure Vault URL or get a working docker instance");
            return null;
        }

        boolean needToStart = !ConfigUtils.isPropertyPresent(URL_CONFIG_KEY);
        if (!needToStart) {
            log.debug("Not starting devservices for default Vault client as url have been provided");
            return null;
        }

        String token = RandomStringUtils.randomAlphanumeric(10);

        DockerImageName dockerImageName = DockerImageName.parse(devServicesConfig.imageName.orElse(VAULT_IMAGE))
                .asCompatibleSubstituteFor(VAULT_IMAGE);
        FixedPortVaultContainer vaultContainer = new FixedPortVaultContainer(dockerImageName, devServicesConfig.port)
                .withVaultToken(token);

        if (devServicesConfig.transitEnabled) {
            vaultContainer.withInitCommand("secrets enable transit");
        }

        if (devServicesConfig.pkiEnabled) {
            vaultContainer.withInitCommand("secrets enable pki");
        }

        vaultContainer.start();

        String url = "http://" + vaultContainer.getHost() + ":" + vaultContainer.getPort();
        return new StartResult(url, token,
                new Closeable() {
                    @Override
                    public void close() {
                        vaultContainer.close();
                    }
                });
    }

    private static class StartResult {
        private final String url;
        private final String clientToken;
        private final Closeable closeable;

        public StartResult(String url, String clientToken, Closeable closeable) {
            this.url = url;
            this.clientToken = clientToken;
            this.closeable = closeable;
        }
    }

    private static class FixedPortVaultContainer extends VaultContainer<FixedPortVaultContainer> {
        OptionalInt fixedExposedPort;

        public FixedPortVaultContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
        }

        @Override
        protected void configure() {
            super.configure();
            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), VAULT_EXPOSED_PORT);
            } else {
                addExposedPort(VAULT_EXPOSED_PORT);
            }
        }

        public int getPort() {
            if (fixedExposedPort.isPresent()) {
                return fixedExposedPort.getAsInt();
            }
            return super.getMappedPort(VAULT_EXPOSED_PORT);
        }
    }
}

package io.quarkus.vault.deployment;

import java.io.Closeable;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.vault.runtime.VaultVersions;
import io.quarkus.vault.runtime.config.DevServicesConfig;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;

public class DevServicesVaultProcessor {
    private static final Logger log = Logger.getLogger(DevServicesVaultProcessor.class);
    private static final String VAULT_IMAGE = "vault:" + VaultVersions.VAULT_TEST_VERSION;
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-vault";
    private static final String DEV_SERVICE_TOKEN = "root";
    private static final int VAULT_EXPOSED_PORT = 8200;
    private static final String CONFIG_PREFIX = "quarkus.vault.";
    private static final String URL_CONFIG_KEY = CONFIG_PREFIX + "url";
    private static final String AUTH_CONFIG_PREFIX = CONFIG_PREFIX + "authentication.";
    private static final String CLIENT_TOKEN_CONFIG_KEY = AUTH_CONFIG_PREFIX + "client-token";
    private static final ContainerLocator vaultContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL, VAULT_EXPOSED_PORT);
    private static volatile Closeable closeable;
    private static volatile DevServicesConfig capturedDevServicesConfiguration;
    private static volatile boolean first = true;
    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
    public void startVaultContainers(BuildProducer<DevServicesConfigResultBuildItem> devConfig, VaultBuildTimeConfig config,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LaunchModeBuildItem launchMode,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig) {

        DevServicesConfig currentDevServicesConfiguration = config.devservices;

        // figure out if we need to shut down and restart any existing Vault container
        // if not and the Vault container have already started we just return
        if (closeable != null) {
            boolean restartRequired = !currentDevServicesConfiguration.equals(capturedDevServicesConfiguration);
            if (!restartRequired) {
                return;
            }
            try {
                closeable.close();
            } catch (Throwable e) {
                log.error("Failed to stop Vault container", e);
            }
            closeable = null;
            capturedDevServicesConfiguration = null;
        }

        capturedDevServicesConfiguration = currentDevServicesConfiguration;

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Vault Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            VaultInstance vaultInstance = startContainer(currentDevServicesConfiguration, launchMode,
                    devServicesConfig.timeout);
            if (vaultInstance != null) {
                closeable = vaultInstance.getCloseable();

                devConfig.produce(new DevServicesConfigResultBuildItem(URL_CONFIG_KEY, vaultInstance.url));
                devConfig.produce(new DevServicesConfigResultBuildItem(CLIENT_TOKEN_CONFIG_KEY, vaultInstance.clientToken));

                if (vaultInstance.isOwner()) {
                    log.info("Dev Services for Vault started.");
                    log.infof("Other Quarkus applications in dev mode will find the "
                            + "instance automatically. For Quarkus applications in production mode, you can connect to"
                            + " this by starting your application with -D%s=%s -D%s=%s",
                            URL_CONFIG_KEY, vaultInstance.url, CLIENT_TOKEN_CONFIG_KEY, vaultInstance.clientToken);
                }
            }
            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (closeable != null) {
                        try {
                            closeable.close();
                        } catch (Throwable t) {
                            log.error("Failed to stop Vault container", t);
                        }
                        closeable = null;
                        log.info("Dev Services for Vault shut down.");
                    }
                    first = true;
                    capturedDevServicesConfiguration = null;
                }
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
    }

    private VaultInstance startContainer(DevServicesConfig devServicesConfig, LaunchModeBuildItem launchMode,
            Optional<Duration> timeout) {
        if (!devServicesConfig.enabled) {
            // explicitly disabled
            log.debug("Not starting devservices for Vault as it has been disabled in the config");
            return null;
        }

        boolean needToStart = !ConfigUtils.isPropertyPresent(URL_CONFIG_KEY);
        if (!needToStart) {
            log.debug("Not starting devservices for default Vault client as url has been provided");
            return null;
        }

        if (!isDockerWorking.getAsBoolean()) {
            log.warn("Please configure Vault URL or get a working docker instance");
            return null;
        }

        DockerImageName dockerImageName = DockerImageName.parse(devServicesConfig.imageName.orElse(VAULT_IMAGE))
                .asCompatibleSubstituteFor(VAULT_IMAGE);
        ConfiguredVaultContainer vaultContainer = new ConfiguredVaultContainer(dockerImageName, devServicesConfig.port,
                devServicesConfig.serviceName)
                        .withVaultToken(DEV_SERVICE_TOKEN);

        vaultContainer.withNetwork(Network.SHARED);

        if (devServicesConfig.transitEnabled) {
            vaultContainer.withInitCommand("secrets enable transit");
        }

        if (devServicesConfig.pkiEnabled) {
            vaultContainer.withInitCommand("secrets enable pki");
        }

        devServicesConfig.initCommands.ifPresent(initCommands -> initCommands.forEach(vaultContainer::withInitCommand));

        final Supplier<VaultInstance> defaultVaultInstanceSupplier = () -> {
            // Starting Vault
            timeout.ifPresent(vaultContainer::withStartupTimeout);
            vaultContainer.start();

            return new VaultInstance(
                    vaultContainer.getHost(),
                    vaultContainer.getPort(),
                    DEV_SERVICE_TOKEN,
                    vaultContainer::close);
        };

        return vaultContainerLocator
                .locateContainer(devServicesConfig.serviceName, devServicesConfig.shared, launchMode.getLaunchMode())
                .map(containerAddress -> new VaultInstance(containerAddress.getHost(), containerAddress.getPort(),
                        DEV_SERVICE_TOKEN, null))
                .orElseGet(defaultVaultInstanceSupplier);
    }

    private static class VaultInstance {
        private final String url;
        private final String clientToken;
        private final Closeable closeable;

        public VaultInstance(String host, int port, String clientToken, Closeable closeable) {
            this("http://" + host + ":" + port, clientToken, closeable);
        }

        public VaultInstance(String url, String clientToken, Closeable closeable) {
            this.url = url;
            this.clientToken = clientToken;
            this.closeable = closeable;
        }

        public boolean isOwner() {
            return closeable != null;
        }

        public Closeable getCloseable() {
            return closeable;
        }
    }

    private static class ConfiguredVaultContainer extends VaultContainer<ConfiguredVaultContainer> {
        OptionalInt fixedExposedPort;

        public ConfiguredVaultContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, String serviceName) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            withNetwork(Network.SHARED);
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
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

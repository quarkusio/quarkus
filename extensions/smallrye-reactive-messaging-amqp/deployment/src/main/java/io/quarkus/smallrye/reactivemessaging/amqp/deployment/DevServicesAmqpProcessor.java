package io.quarkus.smallrye.reactivemessaging.amqp.deployment;

import java.io.Closeable;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts a AMQP 1.0 broker as dev service if needed.
 * It uses https://quay.io/repository/artemiscloud/activemq-artemis-broker as image.
 * See https://artemiscloud.io/ for details.
 */
public class DevServicesAmqpProcessor {

    private static final Logger log = Logger.getLogger(DevServicesAmqpProcessor.class);

    /**
     * Label to add to shared Dev Service for AMQP running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-amqp";

    private static final int AMQP_PORT = 5672;

    private static final ContainerLocator amqpContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL, AMQP_PORT);
    private static final String AMQP_HOST_PROP = "amqp-host";
    private static final String AMQP_PORT_PROP = "amqp-port";
    private static final String AMQP_USER_PROP = "amqp-user";
    private static final String AMQP_PASSWORD_PROP = "amqp-password";

    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    static volatile Closeable closeable;
    static volatile AmqpDevServiceCfg cfg;
    static volatile boolean first = true;

    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class)
    public DevServicesAmqpBrokerBuildItem startAmqpDevService(
            LaunchModeBuildItem launchMode,
            AmqpBuildTimeConfig amqpClientBuildTimeConfig,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfiguration,
            BuildProducer<DevServicesNativeConfigResultBuildItem> devServicePropertiesProducer,
            BuildProducer<ServiceStartBuildItem> serviceStartBuildItemBuildProducer) {

        AmqpDevServiceCfg configuration = getConfiguration(amqpClientBuildTimeConfig);

        if (closeable != null) {
            boolean shouldShutdownTheBroker = launchMode.getLaunchMode() == LaunchMode.TEST;
            if (!shouldShutdownTheBroker) {
                shouldShutdownTheBroker = !configuration.equals(cfg);
            }
            if (!shouldShutdownTheBroker) {
                return null;
            }
            shutdownBroker();
            cfg = null;
        }

        AmqpBroker broker = startAmqpBroker(configuration, launchMode);
        DevServicesAmqpBrokerBuildItem artemis = null;
        if (broker != null) {
            closeable = broker.getCloseable();
            runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(AMQP_HOST_PROP, broker.host));
            runTimeConfiguration
                    .produce(new RunTimeConfigurationDefaultBuildItem(AMQP_PORT_PROP, Integer.toString(broker.port)));
            runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(AMQP_USER_PROP, broker.user));
            runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(AMQP_PASSWORD_PROP, broker.password));

            artemis = new DevServicesAmqpBrokerBuildItem(broker.host, broker.port, broker.user, broker.password);
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (closeable != null) {
                    shutdownBroker();
                }
                first = true;
                closeable = null;
                cfg = null;
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
        }
        cfg = configuration;

        if (broker != null) {
            if (broker.isOwner()) {
                log.infof(
                        "Dev Services for AMQP started. Other Quarkus applications in dev mode will find the "
                                + "broker automatically. For Quarkus applications in production mode, you can connect to"
                                + " this by starting your application with -Damqp.host=%s -Damqp.port=%d -Damqp.user=%s -Damqp.password=%s",
                        broker.host, broker.port, broker.user, broker.password);
                devServicePropertiesProducer
                        .produce(new DevServicesNativeConfigResultBuildItem(AMQP_HOST_PROP, broker.host));
                devServicePropertiesProducer
                        .produce(new DevServicesNativeConfigResultBuildItem(AMQP_PORT_PROP,
                                Integer.toString(broker.port)));
                devServicePropertiesProducer
                        .produce(new DevServicesNativeConfigResultBuildItem(AMQP_USER_PROP, broker.user));
                devServicePropertiesProducer
                        .produce(new DevServicesNativeConfigResultBuildItem(AMQP_PASSWORD_PROP, broker.password));
            }
        }

        return artemis;
    }

    private void shutdownBroker() {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
                log.error("Failed to stop the AMQP broker", e);
            } finally {
                closeable = null;
            }
        }
    }

    private AmqpBroker startAmqpBroker(AmqpDevServiceCfg config, LaunchModeBuildItem launchMode) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting dev services for AMQP, as it has been disabled in the config.");
            return null;
        }

        // Check if amqp.port or amqp.host are set
        if (ConfigUtils.isPropertyPresent(AMQP_HOST_PROP) || ConfigUtils.isPropertyPresent(AMQP_PORT_PROP)) {
            log.debug("Not starting dev services for AMQP, the amqp.host and/or amqp.port are configured.");
            return null;
        }

        // Verify that we have AMQP channels without host and port
        if (!hasAmqpChannelWithoutHostAndPort()) {
            log.debug("Not starting dev services for AMQP, all the channels are configured.");
            return null;
        }

        if (!isDockerWorking.getAsBoolean()) {
            log.warn("Docker isn't working, please configure the AMQP broker location.");
            return null;
        }

        final Supplier<AmqpBroker> defaultAmqpBrokerSupplier = () -> {
            // Starting the broker
            ArtemisContainer container = new ArtemisContainer(
                    DockerImageName.parse(config.imageName),
                    config.extra,
                    config.fixedExposedPort,
                    launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null);

            container.start();

            return new AmqpBroker(
                    container.getHost(),
                    container.getPort(),
                    DEFAULT_USER,
                    DEFAULT_PASSWORD,
                    container::close);
        };

        return amqpContainerLocator.locateContainer(config.serviceName, config.shared, launchMode.getLaunchMode())
                .map(containerAddress -> new AmqpBroker(containerAddress.getHost(), containerAddress.getPort(), "admin",
                        "admin", null))
                .orElseGet(defaultAmqpBrokerSupplier);
    }

    private boolean hasAmqpChannelWithoutHostAndPort() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isConnector) {
                String connectorValue = config.getValue(name, String.class);
                boolean isAmqp = connectorValue.equalsIgnoreCase("smallrye-amqp");
                boolean hasHost = ConfigUtils.isPropertyPresent(name.replace(".connector", ".host"));
                boolean hasPort = ConfigUtils.isPropertyPresent(name.replace(".connector", ".port"));
                isConfigured = isAmqp && (hasHost || hasPort);
            }

            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    private AmqpDevServiceCfg getConfiguration(AmqpBuildTimeConfig cfg) {
        AmqpDevServicesBuildTimeConfig devServicesConfig = cfg.devservices;
        return new AmqpDevServiceCfg(devServicesConfig);
    }

    private static class AmqpBroker {
        private final Closeable closeable;
        private final String host;
        private final int port;
        private final String user;
        private final String password;

        public AmqpBroker(String host, int port, String user, String password, Closeable closeable) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
            this.closeable = closeable;
        }

        public boolean isOwner() {
            return closeable != null;
        }

        public Closeable getCloseable() {
            return closeable;
        }
    }

    private static final class AmqpDevServiceCfg {
        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final String extra;
        private final boolean shared;
        private final String serviceName;

        public AmqpDevServiceCfg(AmqpDevServicesBuildTimeConfig devServicesConfig) {
            this.devServicesEnabled = devServicesConfig.enabled.orElse(true);
            this.imageName = devServicesConfig.imageName;
            this.fixedExposedPort = devServicesConfig.port.orElse(0);
            this.extra = devServicesConfig.extraArgs;
            this.shared = devServicesConfig.shared;
            this.serviceName = devServicesConfig.serviceName;
            ;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AmqpDevServiceCfg that = (AmqpDevServiceCfg) o;
            return devServicesEnabled == that.devServicesEnabled && Objects.equals(imageName, that.imageName)
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, fixedExposedPort);
        }
    }

    /**
     * Container configuring and starting the Artemis broker.
     */
    private static final class ArtemisContainer extends GenericContainer<ArtemisContainer> {

        private final int port;

        private ArtemisContainer(DockerImageName dockerImageName, String extra, int fixedExposedPort, String serviceName) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            withNetwork(Network.SHARED);
            withExposedPorts(AMQP_PORT);
            withEnv("AMQ_USER", DEFAULT_USER);
            withEnv("AMQ_PASSWORD", DEFAULT_PASSWORD);
            withEnv("AMQ_EXTRA_ARGS", extra);
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
            if (dockerImageName.getRepository().equals("artemiscloud/activemq-artemis-broker")) {
                waitingFor(Wait.forLogMessage(".*AMQ241004.*", 1)); // Artemis console available.
            } else {
                throw new IllegalArgumentException("Only artemiscloud/activemq-artemis-broker images are supported");
            }
        }

        @Override
        protected void configure() {
            super.configure();
            if (port > 0) {
                addFixedExposedPort(port, AMQP_PORT);
            }
        }

        public int getPort() {
            return getMappedPort(AMQP_PORT);
        }
    }
}

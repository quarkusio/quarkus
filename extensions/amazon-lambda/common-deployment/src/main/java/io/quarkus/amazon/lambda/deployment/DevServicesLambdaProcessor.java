package io.quarkus.amazon.lambda.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.quarkus.amazon.lambda.runtime.LambdaHotReplacementRecorder;
import io.quarkus.amazon.lambda.runtime.MockEventServer;
import io.quarkus.amazon.lambda.runtime.MockEventServerConfig;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsLiveReloadSupportedByLaunchMode;
import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.runtime.LaunchMode;

public class DevServicesLambdaProcessor {
    private static final Logger log = Logger.getLogger(DevServicesLambdaProcessor.class);

    @BuildStep(onlyIf = IsLiveReloadSupportedByLaunchMode.class)
    @Record(STATIC_INIT)
    public void enableHotReplacementChecker(LaunchModeBuildItem launchMode,
            LambdaHotReplacementRecorder recorder,
            LambdaObjectMapperInitializedBuildItem dependency) {
        if (launchMode.getLaunchMode().isDevOrTest()) {
            if (!legacyTestingEnabled()) {
                recorder.enable();
            }
        }
    }

    private boolean legacyTestingEnabled() {
        try {
            Class.forName("io.quarkus.amazon.lambda.test.LambdaClient");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Produce(ServiceStartBuildItem.class)
    @BuildStep(onlyIfNot = IsProduction.class) // This is required for testing so run it even if devservices.enabled=false
    public void startEventServer(LaunchModeBuildItem launchModeBuildItem,
            LambdaBuildConfig config,
            Optional<EventServerOverrideBuildItem> override,
            BuildProducer<DevServicesResultBuildItem> devServicePropertiesProducer) {
        LaunchMode launchMode = launchModeBuildItem.getLaunchMode();
        if (!launchMode.isDevOrTest())
            return;
        if (legacyTestingEnabled())
            return;
        if (!config.mockEventServer().enabled()) {
            return;
        }

        MockEventServer server;
        if (override.isPresent()) {
            server = override.get().getServer().get();
        } else {
            server = new MockEventServer();
        }

        boolean isTest = launchMode == LaunchMode.TEST;
        String portPropertySuffix = isTest ? "test-port" : "dev-port";
        String propName = "quarkus.lambda.mock-event-server." + portPropertySuffix;

        // No compose support, and no using of external services, so no need to discover existing services

        DevServicesResultBuildItem buildItem = DevServicesResultBuildItem.owned().feature(Feature.AMAZON_LAMBDA)
                .serviceName(Feature.AMAZON_LAMBDA.getName())
                .serviceConfig(config)
                .startable(() -> new StartableEventServer(server, propName, isTest))
                .highPriorityConfig(Set.of(propName)) // Pass through the external config for the port, so that it can be overridden if it's an ephemeral port
                .configProvider(
                        Map.of(propName, s -> String.valueOf(s.getExposedPort()),
                                AmazonLambdaApi.QUARKUS_INTERNAL_AWS_LAMBDA_TEST_API,
                                StartableEventServer::getConnectionInfo))
                .build();

        devServicePropertiesProducer.produce(buildItem);

    }

    private static class StartableEventServer implements Startable {
        private final MockEventServer server;
        private final String propName;
        private final boolean isTest;

        public StartableEventServer(MockEventServer server, String propName, boolean isTest) {
            this.server = server;
            this.propName = propName;
            this.isTest = isTest;
        }

        @Override
        public void start() {
            // Technically, we shouldn't peek at the runtime config, but every dev service does it
            // However, we won't get defaults, so we need to fill our own in
            int port = isTest ? Integer.parseInt(MockEventServerConfig.TEST_PORT)
                    : Integer.parseInt(MockEventServerConfig.DEV_PORT);
            int configuredPort = ConfigProvider.getConfig().getOptionalValue(propName, Integer.class)
                    .or(() -> Optional.of(port))
                    .get();

            server.start(configuredPort);
            log.debugf("Starting event server on port %d", configuredPort);
        }

        public int getExposedPort() {
            return server.getPort();
        }

        @Override
        public String getConnectionInfo() {
            return "localhost:" + getExposedPort() + MockEventServer.BASE_PATH;
        }

        @Override
        public String getContainerId() {
            return null;
        }

        @Override
        public void close() throws IOException {
            server.close();
        }
    }
}

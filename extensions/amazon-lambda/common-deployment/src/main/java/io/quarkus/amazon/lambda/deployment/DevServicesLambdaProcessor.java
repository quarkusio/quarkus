package io.quarkus.amazon.lambda.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.quarkus.amazon.lambda.runtime.LambdaHotReplacementRecorder;
import io.quarkus.amazon.lambda.runtime.MockEventServer;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
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

    @BuildStep(onlyIfNot = IsNormal.class)
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
    @BuildStep(onlyIfNot = IsNormal.class) // This is required for testing so run it even if devservices.enabled=false
    public void startEventServer(LaunchModeBuildItem launchModeBuildItem,
            LambdaConfig config,
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

        int configuredPort = launchMode == LaunchMode.TEST ? config.mockEventServer().testPort()
                : config.mockEventServer().devPort();
        String portPropertySuffix = launchMode == LaunchMode.TEST ? "test-port" : "dev-port";
        String propName = "quarkus.lambda.mock-event-server." + portPropertySuffix;

        // No compose support, and no using of external services, so no need to discover existing services

        DevServicesResultBuildItem buildItem = DevServicesResultBuildItem.owned().feature(Feature.AMAZON_LAMBDA)
                .serviceName(Feature.AMAZON_LAMBDA.getName())
                .serviceConfig(config)
                .startable(() -> new StartableEventServer(
                        server, configuredPort))
                .configProvider(
                        Map.of(propName, s -> String.valueOf(s.getExposedPort()),
                                AmazonLambdaApi.QUARKUS_INTERNAL_AWS_LAMBDA_TEST_API,
                                StartableEventServer::getConnectionInfo))
                .build();

        devServicePropertiesProducer.produce(buildItem);

    }

    private static class StartableEventServer implements Startable {

        private final MockEventServer server;
        private final int configuredPort;

        public StartableEventServer(MockEventServer server, int configuredPort) {
            this.server = server;
            this.configuredPort = configuredPort;
        }

        @Override
        public void start() {
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

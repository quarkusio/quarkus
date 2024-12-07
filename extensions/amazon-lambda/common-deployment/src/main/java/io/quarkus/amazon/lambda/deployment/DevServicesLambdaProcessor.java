package io.quarkus.amazon.lambda.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

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
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.runtime.LaunchMode;

public class DevServicesLambdaProcessor {
    private static final Logger log = Logger.getLogger(DevServicesLambdaProcessor.class);

    static MockEventServer server;
    static LaunchMode startMode;

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
    public void startEventServer(LaunchModeBuildItem launchMode,
            LambdaConfig config,
            Optional<EventServerOverrideBuildItem> override,
            BuildProducer<DevServicesResultBuildItem> devServicePropertiesProducer,
            CuratedApplicationShutdownBuildItem closeBuildItem)
            throws Exception {
        if (!launchMode.getLaunchMode().isDevOrTest())
            return;
        if (legacyTestingEnabled())
            return;
        if (!config.mockEventServer().enabled()) {
            return;
        }
        if (server != null) {
            return;
        }
        Supplier<MockEventServer> supplier = null;
        if (override.isPresent()) {
            supplier = override.get().getServer();
        } else {
            supplier = () -> new MockEventServer();
        }

        server = supplier.get();
        int port = launchMode.getLaunchMode() == LaunchMode.TEST ? config.mockEventServer().testPort()
                : config.mockEventServer().devPort();
        startMode = launchMode.getLaunchMode();
        server.start(port);
        int actualPort = server.getPort();
        String baseUrl = "localhost:" + actualPort + MockEventServer.BASE_PATH;
        Map<String, String> properties = new HashMap<>();
        properties.put(AmazonLambdaApi.QUARKUS_INTERNAL_AWS_LAMBDA_TEST_API, baseUrl);

        if (actualPort != port) {
            String portPropertyValue = String.valueOf(actualPort);
            String portPropertySuffix = launchMode.getLaunchMode() == LaunchMode.TEST ? "test-port" : "dev-port";
            String propName = "quarkus.lambda.mock-event-server." + portPropertySuffix;
            System.setProperty(propName, portPropertyValue);
        }

        devServicePropertiesProducer.produce(
                new DevServicesResultBuildItem(Feature.AMAZON_LAMBDA.getName(), null, properties));
        Runnable closeTask = () -> {
            if (server != null) {
                try {
                    server.close();
                } catch (Throwable e) {
                    log.error("Failed to stop the Lambda Mock Event Server", e);
                } finally {
                    server = null;
                }
            }
            startMode = null;
            server = null;
        };
        closeBuildItem.addCloseTask(closeTask, true);
    }
}

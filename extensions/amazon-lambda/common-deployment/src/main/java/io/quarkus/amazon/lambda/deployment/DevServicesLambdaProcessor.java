package io.quarkus.amazon.lambda.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Optional;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.quarkus.amazon.lambda.runtime.LambdaHotReplacementRecorder;
import io.quarkus.amazon.lambda.runtime.MockEventServer;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RuntimeApplicationShutdownBuildItem;
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
    @BuildStep(onlyIfNot = IsNormal.class)
    public void startEventServer(LaunchModeBuildItem launchMode,
            LambdaConfig config,
            Optional<EventServerOverrideBuildItem> override,
            BuildProducer<DevServicesConfigResultBuildItem> devServicePropertiesProducer,
            BuildProducer<RuntimeApplicationShutdownBuildItem> runtimeApplicationShutdownBuildItemBuildProducer)
            throws Exception {
        if (!launchMode.getLaunchMode().isDevOrTest())
            return;
        if (legacyTestingEnabled())
            return;
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
        int port = launchMode.getLaunchMode() == LaunchMode.TEST ? config.mockEventServer.testPort
                : config.mockEventServer.devPort;
        startMode = launchMode.getLaunchMode();
        server.start(port);
        String baseUrl = "localhost:" + port + MockEventServer.BASE_PATH;
        devServicePropertiesProducer.produce(
                new DevServicesConfigResultBuildItem(AmazonLambdaApi.QUARKUS_INTERNAL_AWS_LAMBDA_TEST_API, baseUrl));
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
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
        ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
        if (launchMode.isTest()) {
            runtimeApplicationShutdownBuildItemBuildProducer.produce(new RuntimeApplicationShutdownBuildItem(closeTask));
        }
    }
}

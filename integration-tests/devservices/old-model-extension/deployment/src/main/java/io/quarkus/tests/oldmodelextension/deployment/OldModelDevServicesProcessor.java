package io.quarkus.tests.oldmodelextension.deployment;

import static io.quarkus.tests.oldmodelextension.Constants.OLD_MODEL_EXTENSION_BASE_URL;
import static io.quarkus.tests.oldmodelextension.Constants.OLD_MODEL_EXTENSION_STATIC_THING;

import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;

@SuppressWarnings("deprecation")
public class OldModelDevServicesProcessor {

    private static final Logger log = Logger.getLogger(OldModelDevServicesProcessor.class);

    private static final String FEATURE = "OldModel";
    private static final int HTTPD_PORT = 80;

    static volatile RunningDevService devService;
    static volatile boolean first = true;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
    public DevServicesResultBuildItem createContainer(CuratedApplicationShutdownBuildItem closeBuildItem) {

        if (devService != null) {
            return devService.toBuildItem();
        }

        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("httpd"))
                .withReuse(true)
                .withExposedPorts(HTTPD_PORT);
        container.start();

        String baseUrl = "http://" + container.getHost() + ":" + container.getMappedPort(HTTPD_PORT);

        devService = new RunningDevService(
                FEATURE,
                container.getContainerId(),
                container::stop,
                Map.of(OLD_MODEL_EXTENSION_BASE_URL, baseUrl,
                        OLD_MODEL_EXTENSION_STATIC_THING, "old model value"));

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devService != null) {
                    shutdownOldModel();
                }
                first = true;
                devService = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }

        return devService.toBuildItem();
    }

    private void shutdownOldModel() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the old model dev service", e);
            } finally {
                devService = null;
            }
        }
    }
}

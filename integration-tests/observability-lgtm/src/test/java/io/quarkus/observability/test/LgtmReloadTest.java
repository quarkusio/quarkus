package io.quarkus.observability.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;

import io.quarkus.devservices.common.Labels;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesRegistry;
import io.quarkus.observability.test.support.ConfigEndpoint;
import io.quarkus.observability.test.support.ReloadEndpoint;
import io.quarkus.observability.test.utils.GrafanaClient;
import io.quarkus.test.QuarkusDevModeTest;

/**
 * Tests hot reload of observability dev services (issue #57067).
 * <p>
 * The key fix from the related PR is that observability dev services now
 * receive the same process-ownership labels ({@code QUARKUS_PROCESS_UUID},
 * {@code QUARKUS_LAUNCH_MODE}) that {@code DevServicesProcessor} applies to
 * other Testcontainers-backed dev services. Without them,
 * {@code ContainerLocator} would rediscover this JVM's own containers as
 * external shared services during reload and stop them.
 */
@DisabledOnOs(OS.WINDOWS)
@Tag("devmode")
public class LgtmReloadTest extends LgtmTestHelper {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(ReloadEndpoint.class, ConfigEndpoint.class)
                            .addAsResource("application.properties",
                                    "application.properties"));

    @Override
    protected String grafanaEndpoint() {
        return GrafanaClient.endpoint();
    }

    @Override
    protected String prometheusEndpoint() {
        return null;
    }

    @Override
    protected String tempoEndpoint() {
        return null;
    }

    @Test
    public void devServiceContainersShouldBeLabeledForOwnershipAndSurviveReload() {
        // 1. Initial poke: verifies dev mode + dev services are up, and captures
        //    that Grafana / Prometheus / Tempo have data flowing.
        poke("/reload");

        // 2. Ownership-labels assertion: every observability dev service container
        //    spawned by this JVM must carry the process-uuid + launch-mode labels.
        //    Before the fix (#57067), ConfigureUtil.configureLabels never reached
        //    the observability Container abstraction, so these labels were missing
        //    and ContainerLocator treated the containers as "external" on reload.
        assertOwnershipLabelsApplied();

        // 3. Hot reload: modify source file → Quarkus restarts,
        //    observability dev services close() + start() behind the scenes.
        test.modifySourceFile(ReloadEndpoint.class, s -> s.replace("/reload", "/new"));
        poke("/new");

        // 4. Modify resource → another reload cycle
        test.modifyResourceFile("application.properties", s -> s.replace("timeout=PT3M", "timeout=PT4M"));
        poke("/new");

        // 5. Labels still present after reload – proof the JVM's own containers
        //    were not rediscovered (and possibly stopped) by ContainerLocator.
        assertOwnershipLabelsApplied();
    }

    /**
     * Lists Docker containers that carry {@link Labels#QUARKUS_DEV_SERVICE} and
     * asserts at least one of them has this JVM's own
     * {@link RunningDevServicesRegistry#APPLICATION_UUID} and DEVELOPMENT launch
     * mode set as labels.
     */
    private void assertOwnershipLabelsApplied() {
        List<Container> quarkusDevServiceContainers = DockerClientFactory.lazyClient()
                .listContainersCmd()
                .withLabelFilter(Labels.QUARKUS_DEV_SERVICE)
                .exec();

        assertThat(quarkusDevServiceContainers)
                .describedAs("Expected at least one Quarkus dev service container in Docker")
                .isNotEmpty();

        String thisProcessUuid = RunningDevServicesRegistry.APPLICATION_UUID;
        log.infof("Looking for containers labeled with QUARKUS_PROCESS_UUID=%s", thisProcessUuid);

        List<Container> ours = quarkusDevServiceContainers.stream()
                .filter(c -> thisProcessUuid.equals(c.getLabels().get(Labels.QUARKUS_PROCESS_UUID)))
                .collect(Collectors.toList());

        assertThat(ours)
                .describedAs(
                        "Expected at least one dev service container owned by this JVM (process-uuid=%s). "
                                + "Containers found: %s",
                        thisProcessUuid,
                        quarkusDevServiceContainers.stream()
                                .map(c -> c.getLabels().get(Labels.QUARKUS_PROCESS_UUID))
                                .collect(Collectors.toList()))
                .isNotEmpty();

        // Every container owned by this process should also carry the launch-mode label
        for (Container c : ours) {
            String launchMode = c.getLabels().get(Labels.QUARKUS_LAUNCH_MODE);
            assertThat(launchMode)
                    .describedAs("Container %s owned by this JVM should carry QUARKUS_LAUNCH_MODE", c.getId())
                    .isEqualTo("DEVELOPMENT");
        }
    }
}

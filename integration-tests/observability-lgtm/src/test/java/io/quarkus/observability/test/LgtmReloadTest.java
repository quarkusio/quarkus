package io.quarkus.observability.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
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

        // 3. Capture the container IDs that currently belong to this JVM.
        //    This is what we actually expect to SURVIVE the hot reload — the
        //    exact bug before the fix was that ContainerLocator would rediscover
        //    them as "external shared" and stop them.
        Set<String> containerIdsBeforeReload = ourContainerIds();
        assertThat(containerIdsBeforeReload)
                .describedAs("Expected at least one dev service container owned by this JVM before reload")
                .isNotEmpty();
        log.infof("Container IDs owned by this JVM before reload: %s", containerIdsBeforeReload);

        // 4. Hot reload: modify source file → Quarkus restarts,
        //    observability dev services close() + start() behind the scenes.
        test.modifySourceFile(ReloadEndpoint.class, s -> s.replace("/reload", "/new"));
        poke("/new");

        // 5. The SAME container IDs must still be running — this is the actual
        //    regression check. Before the fix, container IDs would disappear
        //    (stopped by ContainerLocator) and then new ones would be started.
        Set<String> containerIdsAfterReload = ourContainerIds();
        log.infof("Container IDs owned by this JVM after reload: %s", containerIdsAfterReload);

        assertThat(containerIdsAfterReload)
                .describedAs("Expected at least one dev service container owned by this JVM after reload")
                .isNotEmpty();

        assertThat(containerIdsAfterReload)
                .describedAs(
                        "All pre-reload containers owned by this JVM should still be running after reload. "
                                + "Before the fix (#57067) ContainerLocator would stop them because "
                                + "ConfigureUtil.configureLabels was never called on the observability "
                                + "Container abstraction.")
                .containsAll(containerIdsBeforeReload);

        // 6. Modify resource → another reload cycle, same survival check
        test.modifyResourceFile("application.properties", s -> s.replace("timeout=PT3M", "timeout=PT4M"));
        poke("/new");

        Set<String> containerIdsAfterSecondReload = ourContainerIds();
        log.infof("Container IDs owned by this JVM after second reload: %s", containerIdsAfterSecondReload);

        assertThat(containerIdsAfterSecondReload)
                .describedAs("All pre-reload containers should survive the second reload cycle too")
                .containsAll(containerIdsAfterReload);

        // 7. Labels still present after both reloads – double proof
        assertOwnershipLabelsApplied();
    }

    /**
     * Returns Docker container IDs of dev services owned by <b>this JVM</b>.
     * <p>
     * Uses Docker's server-side label filter on {@link Labels#QUARKUS_PROCESS_UUID}
     * = this JVM's {@link RunningDevServicesRegistry#APPLICATION_UUID}. This
     * guarantees we only see containers spawned by the current test process —
     * even when the CI Docker daemon hosts containers from other parallel tests
     * that happen to carry {@link Labels#QUARKUS_DEV_SERVICE}.
     */
    private Set<String> ourContainerIds() {
        String thisProcessUuid = RunningDevServicesRegistry.APPLICATION_UUID;
        List<Container> all = DockerClientFactory.lazyClient()
                .listContainersCmd()
                .withLabelFilter(Map.of(Labels.QUARKUS_PROCESS_UUID, thisProcessUuid))
                .exec();
        return all.stream()
                .map(Container::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Asserts that all Docker containers owned by this JVM (matched by
     * {@link RunningDevServicesRegistry#APPLICATION_UUID}) carry both
     * {@link Labels#QUARKUS_PROCESS_UUID} and {@link Labels#QUARKUS_LAUNCH_MODE}
     * with the DEVELOPMENT value.
     * <p>
     * The server-side {@code QUARKUS_PROCESS_UUID=<this-process>} filter ensures
     * containers from other parallel tests on the same CI Docker daemon are
     * never included.
     */
    private void assertOwnershipLabelsApplied() {
        String thisProcessUuid = RunningDevServicesRegistry.APPLICATION_UUID;
        List<Container> ours = DockerClientFactory.lazyClient()
                .listContainersCmd()
                .withLabelFilter(Map.of(Labels.QUARKUS_PROCESS_UUID, thisProcessUuid))
                .exec();

        assertThat(ours)
                .describedAs(
                        "Expected at least one dev service container owned by this JVM (process-uuid=%s).",
                        thisProcessUuid)
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

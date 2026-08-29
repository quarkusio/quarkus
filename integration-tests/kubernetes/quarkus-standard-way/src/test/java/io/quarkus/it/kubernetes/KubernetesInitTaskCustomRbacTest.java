package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesInitTaskCustomRbacTest {

    private static final String NAME = "kubernetes-custom-init-rbac";
    private static final String CUSTOM_ROLE_NAME = "shared-view-jobs";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .setLogFileName("k8s.log")
            .overrideConfigKey("quarkus.kubernetes.init-task-defaults.rbac.name", CUSTOM_ROLE_NAME)
            .overrideConfigKey("quarkus.kubernetes.init-task-defaults.rbac.generate", "false")
            .setForcedDependencies(Arrays.asList(
                    Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-flyway", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        Optional<Job> job = kubernetesList.stream()
                .filter(j -> "Job".equals(j.getKind()) && (NAME + "-flyway-init").equals(j.getMetadata().getName()))
                .map(j -> (Job) j)
                .findAny();
        assertTrue(job.isPresent());

        assertThat(kubernetesList).filteredOn(r -> r instanceof Role).isEmpty();

        Optional<RoleBinding> roleBinding = kubernetesList.stream()
                .filter(r -> r instanceof RoleBinding && CUSTOM_ROLE_NAME.equals(r.getMetadata().getName()))
                .map(r -> (RoleBinding) r)
                .findFirst();
        assertTrue(roleBinding.isPresent());
        assertThat(roleBinding.get().getRoleRef().getName()).isEqualTo(CUSTOM_ROLE_NAME);
        assertThat(roleBinding.get().getRoleRef().getKind()).isEqualTo("Role");
    }
}

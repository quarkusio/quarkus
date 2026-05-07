package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class PanacheNextProcessorAutoTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldAutoConfigureHibernateProcessor() throws Exception {
        final File projectDir = getProjectDir("panache-next-processor-auto");

        BuildResult buildResult = runGradleWrapper(projectDir, "clean", "build");

        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":build"))).isTrue();

        // Verify that the hibernate-processor was auto-configured and ran
        // The metamodel class should be generated in build/generated/sources/annotationProcessor/
        Path metamodelFile = projectDir.toPath()
                .resolve("build/generated/sources/annotationProcessor/java/main/org/acme/MyEntity_.java");
        assertThat(metamodelFile).exists();

        // Verify the metamodel contains expected fields
        String metamodelContent = new String(java.nio.file.Files.readAllBytes(metamodelFile));
        assertThat(metamodelContent).contains("SingularAttribute");
        assertThat(metamodelContent).contains("NAME");
        assertThat(metamodelContent).contains("AMOUNT");
    }
}

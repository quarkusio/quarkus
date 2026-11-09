package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.tools.ExecFileLoader;
import org.junit.jupiter.api.Test;

public class JacocoMultiModuleTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testJacoco() throws Exception {
        final File projectDir = getProjectDir("basic-multi-module-project-jacoco");
        runGradleWrapper(projectDir, "clean", "test");

        File execFile = new File(projectDir, "application/build/jacoco-quarkus.exec");
        assertThat(execFile).exists();

        ExecFileLoader loader = new ExecFileLoader();
        loader.load(execFile);

        Set<String> instrumentedClasses = loader.getExecutionDataStore().getContents().stream()
                .map(ExecutionData::getName)
                .collect(Collectors.toSet());
        Set<String> coveredClasses = loader.getExecutionDataStore().getContents().stream()
                .filter(ExecutionData::hasHits)
                .map(ExecutionData::getName)
                .collect(Collectors.toSet());

        // classes from the application and from the project dependencies,
        // with or without the Quarkus plugin applied, are covered
        assertThat(coveredClasses).contains(
                "org/acme/quarkus/sample/HelloResource",
                "org/acme/common/CommonBean",
                "org/acme/library/LibraryBean");
        // external dependencies are not instrumented
        assertThat(instrumentedClasses).allMatch(className -> className.startsWith("org/acme/"));
    }
}

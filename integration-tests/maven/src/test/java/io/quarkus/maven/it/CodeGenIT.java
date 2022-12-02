package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeTestUtils;

/**
 * <p>
 * NOTE to anyone diagnosing failures in this test, to run a single method use:
 * <p>
 * mvn install -Dit.test=CodeGenIT#methodName
 */
@DisableForNative
public class CodeGenIT extends RunAndCheckMojoTestBase {

    @Test
    public void shouldCompileAndRunWithCodegenEnabled() throws MavenInvocationException, FileNotFoundException {
        testDir = initProject("projects/proto-gen");
        run(true);
        assertThat(DevModeTestUtils.getHttpResponse("/hello")).isEqualTo("Hello, World!");
    }

    @Test
    public void shouldFailToCompileWithCodegenDisabled() throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/proto-gen", "projects/proto-gen-failing");
        final File applicationProps = new File(testDir, "src/main/resources/application.properties");
        filter(applicationProps, Collections.singletonMap("quarkus.grpc.codegen.skip=false", "quarkus.grpc.codegen.skip=true"));
        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult compile = running.execute(List.of("compile"), Collections.emptyMap());
        assertThat(compile.getProcess().waitFor()).isNotZero();
    }
}

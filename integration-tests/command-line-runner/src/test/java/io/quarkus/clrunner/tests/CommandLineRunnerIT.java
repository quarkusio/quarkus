package io.quarkus.clrunner.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.clrunner.tests.support.MavenProcessInvocationResult;
import io.quarkus.clrunner.tests.support.MojoTestBase;
import io.quarkus.clrunner.tests.support.RunningInvoker;

public class CommandLineRunnerIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @Test
    public void testCommandLineRunnerIsBuiltProperlyAndExecutesAsExpected()
            throws MavenInvocationException, IOException, InterruptedException {
        // copy the template
        testDir = initProject("projects/classic", "projects/project-classic-clir");

        // invoke the build
        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap());

        // ensure the build completed successfully
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        // ensure the runner jar was created
        final File targetDir = getTargetDir();
        final List<File> runnerJarFiles = getFilesEndingWith(targetDir, "-runner.jar");
        assertThat(runnerJarFiles).hasSize(1);

        final File runnerJarFile = runnerJarFiles.get(0);
        final String expectedOutputFile = runnerJarFile.getParentFile().getAbsolutePath() + "/dummy.txt";

        // launch the jar we just created by passing the file we expect it write and the some input we expect it to capitalize and place in the output
        final Process process = new ProcessBuilder()
                .directory(runnerJarFile.getParentFile())
                .command("java", "-jar", runnerJarFile.getAbsolutePath(), expectedOutputFile, "a", "b", "c")
                .start();

        // ensure the application completed successfully
        assertThat(process.waitFor()).isEqualTo(0);

        // ensure it created the expected output
        final String lineSep = System.lineSeparator();
        assertThat(new File(expectedOutputFile))
                .exists()
                .hasContent("A" + lineSep + "B" + lineSep + "C" + lineSep);
    }

    private File getTargetDir() {
        return new File(testDir.getAbsoluteFile() + "/target");
    }

    private List<File> getFilesEndingWith(File dir, String suffix) {
        final File[] files = dir.listFiles((d, name) -> name.endsWith(suffix));
        return files != null ? Arrays.asList(files) : new ArrayList<>();
    }
}

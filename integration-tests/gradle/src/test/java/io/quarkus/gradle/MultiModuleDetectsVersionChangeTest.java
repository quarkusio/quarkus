package io.quarkus.gradle;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

public class MultiModuleDetectsVersionChangeTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testVersionChangeIsDetectedInQuarkusApplicationModelTask()
            throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("multi-module-kotlin-project");

        final BuildResult result = runGradleWrapper(projectDir, "quarkusBuild", "--no-build-cache");

        assertSame("SUCCESS", result.getTasks().get(":web:quarkusBuildAppModel"));

        String filePath = projectDir.getPath() + "/port/build.gradle.kts";
        String newLine = "version=\"2.0\"";

        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(System.lineSeparator());
            writer.write(newLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final BuildResult secondBuild = runGradleWrapper(projectDir, "quarkusBuild", "--no-build-cache");
        assertSame("SUCCESS", secondBuild.getTasks().get(":web:quarkusBuildAppModel"));
    }
}

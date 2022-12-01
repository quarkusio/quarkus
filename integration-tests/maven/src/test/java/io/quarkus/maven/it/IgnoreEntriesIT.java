package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@DisableForNative
public class IgnoreEntriesIT extends MojoTestBase {

    @Test
    public void testIgnoreEntries()
            throws MavenInvocationException, IOException, InterruptedException {
        File testDir = initProject("projects/ignore-entries-uber-jar");

        RunningInvoker running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Arrays.asList("compile", "quarkus:build"),
                Collections.emptyMap());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = new File(testDir, "target");
        List<File> jars = getFilesEndingWith(targetDir, ".jar");
        assertThat(jars).hasSize(1);
        assertThat(jars.get(0)).isFile();
        assertThat(jarContains(jars.get(0), "META-INF/swagger-ui-files/swagger-ui-bundle.js.map")).isFalse();
    }

    private boolean jarContains(File jar, String name) throws IOException {
        try (JarFile z = new JarFile(jar)) {
            for (Enumeration<JarEntry> en = z.entries(); en.hasMoreElements();) {
                String fileName = en.nextElement().getName();
                if (name.equalsIgnoreCase(fileName)) {
                    return true;
                }
            }
        }
        return false;
    }
}

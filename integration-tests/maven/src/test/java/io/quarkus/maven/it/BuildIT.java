package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.test.devmode.util.DevModeTestUtils;

@DisableForNative
class BuildIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @Test
    void testConditionalDependencies()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/conditional-dependencies", "projects/conditional-dependencies-build");

        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap());
        assertThat(result.getProcess().waitFor()).isZero();

        final File targetDir = new File(testDir, "runner" + File.separator + "target");
        final File runnerJar = targetDir.toPath().resolve("quarkus-app").resolve("quarkus-run.jar").toFile();
        // make sure the jar can be read by JarInputStream
        ensureManifestOfJarIsReadableByJarInputStream(runnerJar);

        final Path mainLib = targetDir.toPath().resolve("quarkus-app").resolve("lib").resolve("main");
        assertThat(mainLib.resolve("org.acme.acme-quarkus-ext-a-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.acme-quarkus-ext-b-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.acme-quarkus-ext-c-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.acme-quarkus-ext-d-1.0-SNAPSHOT.jar")).doesNotExist();
    }

    @Test
    void testMultiModuleAppRootWithNoSources()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/multimodule-root-no-src", "projects/multimodule-root-no-src-build");

        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Collections.singletonList("install -pl .,html,rest"),
                Collections.emptyMap());
        assertThat(result.getProcess().waitFor()).isZero();

        result = running.execute(Collections.singletonList("quarkus:build -f runner"), Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isZero();

        final File targetDir = new File(testDir, "runner" + File.separator + "target");
        final File runnerJar = targetDir.toPath().resolve("quarkus-app").resolve("quarkus-run.jar").toFile();

        // make sure the jar can be read by JarInputStream
        ensureManifestOfJarIsReadableByJarInputStream(runnerJar);
    }

    @Test
    void testModuleWithBuildProfileInProperty() throws MavenInvocationException, InterruptedException, IOException {
        testDir = initProject("projects/build-mode-quarkus-profile-property");
        build(null);
        launch();
    }

    @Test
    void testModuleWithOverriddenBuildProfile() throws MavenInvocationException, InterruptedException, IOException {
        testDir = initProject("projects/build-mode-quarkus-profile-override");
        build(String.format("-D%s=foo", ProfileManager.QUARKUS_PROFILE_PROP));
        launch();
    }

    private void launch() throws IOException {
        File output = new File(testDir, "target/output.log");
        output.createNewFile();
        Process process = JarRunnerIT.doLaunch(new File(testDir, "target/quarkus-app"), Paths.get("quarkus-run.jar"), output,
                Collections.emptyList()).start();
        try {
            Assertions.assertEquals("hello, from foo", DevModeTestUtils.getHttpResponse("/hello"));
        } finally {
            process.destroy();
        }
    }

    private void build(String arg) throws MavenInvocationException, InterruptedException, IOException {
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);

        final List<String> args = new ArrayList<>(2);
        args.add("package");
        if (arg != null) {
            args.add(arg);
        }
        MavenProcessInvocationResult result = running.execute(args, Collections.emptyMap());
        assertThat(result.getProcess().waitFor()).isZero();
    }

    private void ensureManifestOfJarIsReadableByJarInputStream(File jar) throws IOException {
        try (InputStream fileInputStream = new FileInputStream(jar)) {
            try (JarInputStream stream = new JarInputStream(fileInputStream)) {
                Manifest manifest = stream.getManifest();
                assertThat(manifest).isNotNull();
            }
        }
    }
}

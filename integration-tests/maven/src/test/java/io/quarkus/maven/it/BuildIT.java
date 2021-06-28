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
    void testClassLoaderLinkageError()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/classloader-linkage-error", "projects/classloader-linkage-error-build");
        build();
        for (TestContext context : TestContext.values()) {
            if (context == TestContext.FAST_NO_PREFIX) {
                continue;
            }
            launch(context, "", "hello");
        }
    }

    @Test
    void testModuleWithBuildProfileInProperty() throws MavenInvocationException, InterruptedException, IOException {
        testDir = initProject("projects/build-mode-quarkus-profile-property");
        build();
        launch();
    }

    @Test
    void testModuleWithOverriddenBuildProfile() throws MavenInvocationException, InterruptedException, IOException {
        testDir = initProject("projects/build-mode-quarkus-profile-override");
        build(String.format("-D%s=foo", ProfileManager.QUARKUS_PROFILE_PROP));
        launch();
    }

    @Test
    void testMultiBuildMode() throws MavenInvocationException, InterruptedException, IOException {
        testDir = initProject("projects/multi-build-mode");
        build();

        for (TestContext context : TestContext.values()) {
            if (context == TestContext.FAST_NO_PREFIX) {
                continue;
            }
            launch(context, "foo-", "Foo: hello, from foo-?/MultiSet");
            launch(context, "bar-", "Bar: hello, from bar-FileUtils/?");
            launch(context, "foo-full-", "Foo: hello, from foo-FileUtils/MultiSet");
            launch(context, "bar-empty-", "Bar: hello, from bar-?/?");
        }
    }

    @Test
    void testModulesInProfiles()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/modules-in-profiles");
        build("-Dquarkus.bootstrap.effective-model-builder");
    }

    @Test
    void testMultiBuildModeLaunchedInParallel() throws MavenInvocationException, InterruptedException, IOException {
        testDir = initProject("projects/multi-build-mode-parallel");
        build(true);

        launch(TestContext.FAST_NO_PREFIX, new File(testDir, "module-1"), "foo-1-", "Hello foo 1");
        launch(TestContext.FAST_NO_PREFIX, new File(testDir, "module-1"), "bar-1-", "Hello bar 1");
        launch(TestContext.FAST_NO_PREFIX, new File(testDir, "module-2"), "foo-2-", "Hello foo 2");
        launch(TestContext.FAST_NO_PREFIX, new File(testDir, "module-2"), "bar-2-", "Hello bar 2");
    }

    private void launch() throws IOException {
        launch(TestContext.FAST_NO_PREFIX, "", "hello, from foo");
    }

    private void launch(TestContext context, String outputPrefix, String expectedMessage) throws IOException {
        launch(context, testDir, outputPrefix, expectedMessage);
    }

    private void launch(TestContext context, File testDir, String outputPrefix, String expectedMessage) throws IOException {
        File output = new File(testDir, String.format("target/%s%soutput.log", context.prefix, outputPrefix));
        output.createNewFile();
        Process process = JarRunnerIT
                .doLaunch(new File(testDir, String.format("target/%s%squarkus-app", context.prefix, outputPrefix)),
                        Paths.get(context.jarFileName), output,
                        Collections.emptyList())
                .start();
        try {
            Assertions.assertEquals(expectedMessage, DevModeTestUtils.getHttpResponse("/hello"));
        } finally {
            process.destroy();
        }
    }

    private void build(String... arg) throws MavenInvocationException, InterruptedException, IOException {
        build(false, arg);
    }

    private void build(boolean parallel, String... arg) throws MavenInvocationException, InterruptedException, IOException {
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false, parallel);

        final List<String> args = new ArrayList<>(2);
        args.add("package");
        if (arg.length > 0) {
            for (String a : arg) {
                args.add(a);
            }
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

    enum TestContext {
        FAST_NO_PREFIX("", "quarkus-run.jar"),
        FAST("fast-", "quarkus-run.jar"),
        LEGACY("legacy-", "legacy-runner.jar"),
        UBER("uber-", "uber-runner.jar");

        final String prefix;
        final String jarFileName;

        TestContext(String prefix, String jarFileName) {
            this.prefix = prefix;
            this.jarFileName = jarFileName;
        }
    }
}

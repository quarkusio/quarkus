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
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeClient;

@DisableForNative
class BuildIT extends MojoTestBase {

    private DevModeClient devModeClient = new DevModeClient();

    private RunningInvoker running;
    private File testDir;

    @Test
    void testQuarkusBootstrapWorkspaceDiscovery() throws Exception {
        testDir = initProject("projects/project-with-extension", "projects/project-with-extension-build");
        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running
                .execute(List.of("clean", "compile", "quarkus:build", "-Dquarkus.bootstrap.workspace-discovery",
                        "-Dquarkus.analytics.disabled=true"), Map.of());
        assertThat(result.getProcess().waitFor()).isZero();

        launch(TestContext.FAST_NO_PREFIX, "/app/hello/local-modules", new File(testDir, "runner"), "",
                "[org.acme:acme-common-transitive:1.0-SNAPSHOT, org.acme:acme-common:1.0-SNAPSHOT, org.acme:acme-library:1.0-SNAPSHOT, org.acme:acme-quarkus-ext-deployment:1.0-SNAPSHOT, org.acme:acme-quarkus-ext:1.0-SNAPSHOT]");
    }

    @Test
    void testCustomTestSourceSets()
            throws MavenInvocationException, InterruptedException {
        testDir = initProject("projects/test-source-sets");
        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(List.of("clean", "verify", "-Dquarkus.analytics.disabled=true"),
                Map.of());
        assertThat(result.getProcess().waitFor()).isZero();
    }

    /**
     * Unlike {@link #testCustomTestSourceSets()} above, this test tests something that shouldn't work.
     * The test project in {@link #testCustomTestSourceSets()} configures the compiler plugin and the test
     * plugins with the custom classes locations in addition to adding custom resource directories.
     *
     * This test was added to fix a regression of custom resource location resolution in the
     * {@link io.quarkus.bootstrap.resolver.maven.workspace.LocalProject},
     * which should further be properly refactored to let only the {@link #testCustomTestSourceSets()} work,
     * while this test should be changed to not locate custom resources on the classpath.
     */
    @Test
    void testCustomResourceDir()
            throws MavenInvocationException, InterruptedException {
        testDir = initProject("projects/custom-resources-dir");
        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(List.of("clean", "test",
                "-Dquarkus.analytics.disabled=true"),
                Map.of());
        assertThat(result.getProcess().waitFor()).isZero();

        // with the source generation disabled (this will use a new resolver initialized before bootstrapping the test)
        result = running.execute(List.of("clean", "test",
                "-Dquarkus.analytics.disabled=true", "-Dquarkus.generate-code.skip=true"),
                Map.of());
        assertThat(result.getProcess().waitFor()).isZero();
    }

    @Test
    void testQuarkusMainTest()
            throws MavenInvocationException, InterruptedException, IOException {
        testDir = initProject("projects/basic-command-mode", "projects/basic-command-mode-test");
        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(List.of("clean", "test"),
                Map.of());
        assertThat(result.getProcess().waitFor()).isZero();
        final String log = running.log();
        assertThat(log).contains("ARGS: [one]");
    }

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
        build(String.format("-D%s=foo", "quarkus.profile"));
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

    @Test
    void testCustomManifestAttributes() throws MavenInvocationException, InterruptedException, IOException {
        testDir = initProject("projects/custom-manifest-attributes");
        build();

        File targetDir = new File(testDir, "target");
        File jar = new File(targetDir, "acme-1.0-SNAPSHOT-runner.jar");

        try (InputStream fileInputStream = new FileInputStream(jar)) {
            try (JarInputStream stream = new JarInputStream(fileInputStream)) {
                Manifest manifest = stream.getManifest();
                assertThat(manifest).isNotNull();
                assertThat(manifest.getMainAttributes().getValue("Built-By")).isEqualTo("quarkus-maven-plugin");

                Attributes section = manifest.getAttributes("org.acme");
                assertThat(section).isNotNull();
                assertThat(section.getValue("visibility")).isEqualTo("public");

                section = manifest.getAttributes("org.acme.internal");
                assertThat(section).isNotNull();
                assertThat(section.getValue("visibility")).isEqualTo("private");
            }
        }
    }

    @Test
    void testIdeDevModeBuildPropsPropagation() throws MavenInvocationException, InterruptedException, IOException {
        testDir = initProject("projects/ide-dev-mode-build-props");
        build();
    }

    @Test
    void testMavenExtensionManipulatingPom()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/maven-extension-manipulating-pom/mvn-ext",
                "projects/maven-extension-manipulating-pom/mvn-ext-processed");
        build("install");
        testDir = initProject("projects/maven-extension-manipulating-pom/app",
                "projects/maven-extension-manipulating-pom/app-processed");
        build();
    }

    @Test
    void testFlattenMavenPlugin()
            throws MavenInvocationException, IOException, InterruptedException {
        // in this case the flatten plugin is expected to strip down dependencyManagement and test scoped dependencies
        // which would break Quarkus bootstrap
        testDir = initProject("projects/flatten-maven-plugin", "projects/flatten-maven-plugin-processed");
        build();
    }

    private void launch() throws IOException {
        launch(TestContext.FAST_NO_PREFIX, "", "hello, from foo");
    }

    private void launch(TestContext context, String outputPrefix, String expectedMessage) throws IOException {
        launch(context, testDir, outputPrefix, expectedMessage);
    }

    private void launch(TestContext context, File testDir, String outputPrefix, String expectedMessage) throws IOException {
        launch(context, "/hello", testDir, outputPrefix, expectedMessage);
    }

    private void launch(TestContext context, String path, File testDir, String outputPrefix, String expectedMessage)
            throws IOException {
        File output = new File(testDir, String.format("target/%s%soutput.log", context.prefix, outputPrefix));
        output.createNewFile();
        Process process = JarRunnerIT
                .doLaunch(new File(testDir, String.format("target/%s%squarkus-app", context.prefix, outputPrefix)),
                        Paths.get(context.jarFileName), output,
                        List.of())
                .start();
        try {
            Assertions.assertEquals(expectedMessage, devModeClient.getHttpResponse(path));
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
        Collections.addAll(args, arg);
        MavenProcessInvocationResult result = running.execute(args, Collections.emptyMap());
        int exitCode = result.getProcess().waitFor();
        if (exitCode != 0) {
            System.err.println(running.log()); //dump the log in order to make it easier find error in CI
            assertThat(exitCode).isZero(); // make sure the build fails
        }

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

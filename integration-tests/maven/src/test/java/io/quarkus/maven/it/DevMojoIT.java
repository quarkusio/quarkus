package io.quarkus.maven.it;

import static io.quarkus.maven.it.ApplicationNameAndVersionTestUtil.assertApplicationPropertiesSetCorrectly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.CapabilityErrors;
import io.quarkus.maven.it.continuoustesting.ContinuousTestingMavenTestUtils;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeTestUtils;
import io.restassured.RestAssured;

/**
 * Tests the quarkus:test mojo.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 *         <p>
 *         NOTE to anyone diagnosing failures in this test, to run a single method use:
 *         <p>
 *         mvn install -Dit.test=DevMojoIT#methodName
 */
@DisableForNative
public class DevMojoIT extends LaunchMojoTestBase {

    @Override
    protected ContinuousTestingMavenTestUtils getTestingTestUtils() {
        return new ContinuousTestingMavenTestUtils();
    }

    @Test
    public void testFlattenedPomInTargetDir() throws MavenInvocationException, IOException {
        testDir = initProject("projects/pom-in-target-dir");
        run(true);
        assertThat(DevModeTestUtils.getHttpResponse("/hello")).isEqualTo("Hello from RESTEasy Reactive");
    }

    @Test
    public void testConfigFactoryInAppModuleBannedInCodeGen() throws MavenInvocationException, IOException {
        testDir = initProject("projects/codegen-config-factory", "projects/codegen-config-factory-banned");
        run(true);
        assertThat(DevModeTestUtils.getHttpResponse("/codegen-config/acme-config-factory")).isEqualTo("n/a");
        assertThat(DevModeTestUtils.getHttpResponse("/codegen-config/acme-config-provider")).isEqualTo("n/a");
        assertThat(DevModeTestUtils.getHttpResponse("/runtime-config/acme-config-factory"))
                .isEqualTo("org.acme.AppConfigSourceFactory");
        assertThat(DevModeTestUtils.getHttpResponse("/runtime-config/acme-config-provider"))
                .isEqualTo("org.acme.AppConfigSourceProvider");
    }

    @Test
    public void testConfigFactoryInAppModuleFilteredInCodeGen() throws MavenInvocationException, IOException {
        testDir = initProject("projects/codegen-config-factory", "projects/codegen-config-factory-filtered");
        run(true, "-Dconfig-factory.enabled");
        assertThat(DevModeTestUtils.getHttpResponse("/codegen-config/acme-config-factory"))
                .isEqualTo("org.acme.config.AcmeConfigSourceFactory");
        assertThat(DevModeTestUtils.getHttpResponse("/codegen-config/acme-config-provider"))
                .isEqualTo("org.acme.config.AcmeConfigSourceProvider");
        assertThat(DevModeTestUtils.getHttpResponse("/runtime-config/acme-config-factory"))
                .isEqualTo("org.acme.AppConfigSourceFactory");
        assertThat(DevModeTestUtils.getHttpResponse("/runtime-config/acme-config-provider"))
                .isEqualTo("org.acme.AppConfigSourceProvider");
    }

    @Test
    public void testSystemPropertiesConfig() throws MavenInvocationException, IOException {
        testDir = initProject("projects/dev-mode-sys-props-config");
        run(true);
        assertThat(DevModeTestUtils.getHttpResponse("/hello")).isEqualTo("hello, out there");
    }

    @Test
    public void testEnvironmentVariablesConfig() throws MavenInvocationException, IOException {
        testDir = initProject("projects/dev-mode-env-vars-config");
        run(true);
        assertThat(DevModeTestUtils.getHttpResponse("/hello")).isEqualTo("hello, WORLD");
    }

    @Test
    void testClassLoaderLinkageError()
            throws MavenInvocationException, IOException {
        testDir = initProject("projects/classloader-linkage-error", "projects/classloader-linkage-error-dev");
        run(true);
        assertThat(DevModeTestUtils.getHttpResponse("/hello")).isEqualTo("hello");
    }

    @Test
    public void testCapabilitiesConflict() throws MavenInvocationException, IOException {
        testDir = getTargetDir("projects/capabilities-conflict");
        final File runnerPom = new File(testDir, "runner/pom.xml");
        if (!runnerPom.exists()) {
            fail("Failed to locate runner/pom.xml in " + testDir);
        }
        run(true);

        final CapabilityErrors error = new CapabilityErrors();
        error.addConflict("sunshine", "org.acme:alt-quarkus-ext:1.0-SNAPSHOT");
        error.addConflict("sunshine", "org.acme:acme-quarkus-ext:1.0-SNAPSHOT");
        String response = DevModeTestUtils.getHttpResponse("/hello", true);
        assertThat(response).contains(error.report());

        filter(runnerPom, Map.of("<artifactId>acme-quarkus-ext</artifactId>", "<artifactId>alt-quarkus-ext</artifactId>"));
        assertThat(DevModeTestUtils.getHttpResponse("/hello", false)).isEqualTo("hello");

        filter(runnerPom, Map.of("<artifactId>alt-quarkus-ext</artifactId>", "<artifactId>acme-quarkus-ext</artifactId>"));
        assertThat(DevModeTestUtils.getHttpResponse("/hello", false)).isEqualTo("hello");
    }

    @Test
    public void testCapabilitiesMissing() throws MavenInvocationException, IOException {
        testDir = getTargetDir("projects/capabilities-missing");
        final File runnerPom = new File(testDir, "runner/pom.xml");
        if (!runnerPom.exists()) {
            fail("Failed to locate runner/pom.xml in " + testDir);
        }
        run(true);

        final CapabilityErrors error = new CapabilityErrors();
        error.addMissing("sunshine", "org.acme:acme-quarkus-ext:1.0-SNAPSHOT");
        String response = DevModeTestUtils.getHttpResponse("/hello", true);
        assertThat(response).contains(error.report());

        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {
            writer.write("        <dependency>");
            writer.newLine();
            writer.write("            <groupId>org.acme</groupId>");
            writer.newLine();
            writer.write("            <artifactId>alt-quarkus-ext</artifactId>");
            writer.newLine();
            writer.write("        </dependency>");
            writer.newLine();
        }
        final String acmeDep = buf.toString();
        filter(runnerPom, Collections.singletonMap("<!-- missing -->", acmeDep));
        assertThat(DevModeTestUtils.getHttpResponse("/hello", false)).isEqualTo("hello");
    }

    @Test
    public void testPropertyOverridesTest() throws MavenInvocationException, IOException {
        testDir = getTargetDir("projects/property-overrides");
        runAndCheck("-Dlocal-dep.version=1.0-SNAPSHOT");
    }

    @Test
    public void testSystemPropertyWithSpacesOnCommandLine() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-prop-with-spaces");
        runAndCheck("-Dgreeting=\"1 2 3\"");
        final String greeting = DevModeTestUtils.getHttpResponse("/app/hello/greeting");
        assertThat(greeting).isEqualTo("1 2 3");
    }

    @Test
    public void testCommandModeAppSystemPropArguments() throws MavenInvocationException, IOException {
        testDir = initProject("projects/basic-command-mode", "projects/command-mode-app-args");
        run(false, "-Dquarkus.args='1 2'");

        // Wait until this file exists
        final File done = new File(testDir, "done.txt");
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(20, TimeUnit.MINUTES).until(done::exists);

        // read the log and check the passed in args
        final File log = new File(testDir, "build-command-mode-app-args.log");
        assertThat(log).exists();
        String loggedArgs = extractLoggedArgs(log);
        assertThat(loggedArgs).isEqualTo("ARGS: [1, 2]");
    }

    @Test
    public void testCommandModeAppPomConfigArguments() throws MavenInvocationException, IOException {
        testDir = initProject("projects/command-mode-app-args-plugin-config", "projects/command-mode-app-pom-args");
        run(false);

        // Wait until this file exists
        final File done = new File(testDir, "done.txt");
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(20, TimeUnit.MINUTES).until(done::exists);

        // read the log and check the passed in args
        final File log = new File(testDir, "build-command-mode-app-pom-args.log");
        assertThat(log).exists();
        String loggedArgs = extractLoggedArgs(log);
        assertThat(loggedArgs).isEqualTo("ARGS: [plugin, pom, config]");
    }

    private String extractLoggedArgs(final File log) throws IOException {
        String loggedArgs = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
            String s;
            while ((s = reader.readLine()) != null) {
                // not startsWith() because line might start with ANSI escape sequence (which must be stripped)
                int indexOfARGS = s.indexOf("ARGS: ");
                if (indexOfARGS > -1) {
                    loggedArgs = s.substring(indexOfARGS);
                    break;
                }
            }
        }
        return loggedArgs;
    }

    @Test
    public void testThatClassAppCanRun() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run");
        runAndCheck();

        //make sure that the Class.getPackage() works for app classes
        String pkg = DevModeTestUtils.getHttpResponse("/app/hello/package");
        assertThat(pkg).isEqualTo("org.acme");

        //make sure the proper profile is set
        String profile = DevModeTestUtils.getHttpResponse("/app/hello/profile");
        assertThat(profile).isEqualTo("dev");

        //make sure webjars work
        DevModeTestUtils.getHttpResponse("webjars/jquery-ui/1.13.0/jquery-ui.min.js");

        assertThatOutputWorksCorrectly(running.log());

        assertApplicationPropertiesSetCorrectly();
    }

    @Test
    public void testThatResteasyWithoutUndertowCanRun() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-no-undertow", "projects/project-classic-no-undertow-run");
        run(false);

        //make sure that a simple HTTP GET request always works
        IntStream.range(0, 10).forEach(i -> {
            assertThat(DevModeTestUtils.getStrictHttpResponse("/hello", 200)).isTrue();
        });
    }

    @Test
    public void testThatInitialMavenResourceFilteringWorks() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-resource-filtering", "projects/project-classic-resource-filtering");

        //also test that a zipfile must not be filtered because of nonFilteredFileExtensions configuration
        //as initProject() would already corrupt the zipfile, it has to be created _after_ initProject()
        try (ZipOutputStream zipOut = new ZipOutputStream(
                new FileOutputStream(new File(testDir, "src/main/resources/test.zip")))) {
            ZipEntry zipEntry = new ZipEntry("test.txt");
            zipOut.putNextEntry(zipEntry);
            zipOut.write("test".getBytes());
        }

        run(false);

        //make sure that a simple HTTP GET request always works
        IntStream.range(0, 10).forEach(i -> {
            assertThat(DevModeTestUtils.getStrictHttpResponse("/hello", 200)).isTrue();
        });

        //try to open the copied test.zip (which will fail if it was filtered)
        File copiedTestZipFile = new File(testDir, "target/classes/test.zip");
        assertThat(copiedTestZipFile).exists();
        try (ZipFile zipFile = new ZipFile(copiedTestZipFile)) {
            //everything is fine once we get here (ZipFile is still readable)
        }
    }

    @Test
    public void testThatTheApplicationIsReloadedOnJavaChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-java-change");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        String uuid = UUID.randomUUID().toString();
        filter(source, Collections.singletonMap("return \"hello\";", "return \"" + uuid + "\";"));

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        filter(source, Collections.singletonMap(uuid, "carambar"));

        // Wait until we get "carambar"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("carambar"));
    }

    @Test
    public void testCustomOutputDirSetInProfile() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-custom-output-dir");
        runAndCheck("-PcustomOutputDir");
    }

    @Test
    public void testThatNonExistentSrcDirCanBeAdded() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-java-change");

        File sourceDir = new File(testDir, "src/main/java");
        File sourceDirMoved = new File(testDir, "src/main/java-moved");
        if (!sourceDir.renameTo(sourceDirMoved)) {
            Assertions.fail("move failed");
        }
        //we need this to make run and check work
        File hello = new File(testDir, "src/main/resources/META-INF/resources/app/hello");
        hello.getParentFile().mkdir();
        try (var o = new FileOutputStream(hello)) {
            o.write("hello".getBytes(StandardCharsets.UTF_8));
        }

        runAndCheck();
        hello.delete();
        if (!DevModeTestUtils.getHttpResponse("/app/hello", 404)) {
            Assertions.fail("expected resource to be deleted");
        }
        if (!sourceDirMoved.renameTo(sourceDir)) {
            Assertions.fail("move failed");
        }

        // Wait until we get "hello"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("hello"));
    }

    @Test
    public void testThatInstrumentationBasedReloadWorks() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-inst", "projects/project-instrumentation-reload");
        runAndCheck();

        // Enable instrumentation based reload to begin with
        RestAssured.post("/q/dev-v1/io.quarkus.quarkus-vertx-http/tests/toggle-instrumentation").then().statusCode(200);

        //if there is an instrumentation based reload this will stay the same
        String firstUuid = DevModeTestUtils.getHttpResponse("/app/uuid");

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        String uuid = UUID.randomUUID().toString();
        filter(source, Collections.singletonMap("return \"hello\";", "return \"" + uuid + "\";"));

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains(uuid));

        //verify that this was an instrumentation based reload
        Assertions.assertEquals(firstUuid, DevModeTestUtils.getHttpResponse("/app/uuid"));

        source = new File(testDir, "src/main/java/org/acme/HelloService.java");
        filter(source, Collections.singletonMap("\"Stuart\"", "\"Stuart Douglas\""));

        // Wait until we get "Stuart Douglas"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/name").contains("Stuart Douglas"));

        //this bean observes startup event, so it should be different UUID
        String secondUUid = DevModeTestUtils.getHttpResponse("/app/uuid");
        Assertions.assertNotEquals(secondUUid, firstUuid);

        //now disable instrumentation based restart, and try again
        //change it back to hello
        RestAssured.post("/q/dev-v1/io.quarkus.quarkus-vertx-http/tests/toggle-instrumentation").then().statusCode(200);
        source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        filter(source, Collections.singletonMap("return \"" + uuid + "\";", "return \"hello\";"));

        // Wait until we get "hello"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("hello"));

        //verify that this was not instrumentation based reload
        Assertions.assertNotEquals(secondUUid, DevModeTestUtils.getHttpResponse("/app/uuid"));
        secondUUid = DevModeTestUtils.getHttpResponse("/app/uuid");

        //now re-enable
        //and repeat
        RestAssured.post("/q/dev-v1/io.quarkus.quarkus-vertx-http/tests/toggle-instrumentation").then().statusCode(200);
        source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        filter(source, Collections.singletonMap("return \"hello\";", "return \"" + uuid + "\";"));

        // Wait until we get uuid
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains(uuid));

        //verify that this was an instrumentation based reload
        Assertions.assertEquals(secondUUid, DevModeTestUtils.getHttpResponse("/app/uuid"));

        // verify that add + change results in full reload
        // add a new class
        Files.write(Paths.get(testDir.toString(), "src/main/java/org/acme/AnotherClass.java"),
                "package org.acme;\nclass ItDoesntMatter{}".getBytes());

        // change back to hello
        source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        filter(source, Collections.singletonMap("return \"" + uuid + "\";", "return \"hello\";"));

        // Wait until we get "hello"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("hello"));

        //verify that this was not instrumentation based reload
        Assertions.assertNotEquals(secondUUid, DevModeTestUtils.getHttpResponse("/app/uuid"));
        secondUUid = DevModeTestUtils.getHttpResponse("/app/uuid");

    }

    @Test
    public void testThatSourceChangesAreDetectedOnPomChange() throws Exception {
        testDir = initProject("projects/classic", "projects/project-classic-run-src-and-pom-change");
        runAndCheck();

        // Edit a Java file too
        final File javaSource = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        final String uuid = UUID.randomUUID().toString();
        filter(javaSource, Collections.singletonMap("return \"hello\";", "return \"hello " + uuid + "\";"));

        // edit the application.properties too
        final File applicationProps = new File(testDir, "src/main/resources/application.properties");
        filter(applicationProps, Collections.singletonMap("greeting=bonjour", "greeting=" + uuid + ""));

        // Now edit the pom.xml to trigger the dev mode restart
        final File pomSource = new File(testDir, "pom.xml");
        filter(pomSource, Collections.singletonMap("<!-- insert test dependencies here -->",
                "        <dependency>\n" +
                        "            <groupId>io.quarkus</groupId>\n" +
                        "            <artifactId>quarkus-smallrye-openapi</artifactId>\n" +
                        "        </dependency>"));

        // Wait until we get the updated responses
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("hello " + uuid));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greeting").contains(uuid));

    }

    @Test
    public void testAlternatePom() throws Exception {
        testDir = initProject("projects/classic", "projects/project-classic-alternate-pom");

        File pom = new File(testDir, "pom.xml");
        if (!pom.exists()) {
            throw new IllegalStateException("Failed to locate project's pom.xml at " + pom);
        }
        final String alternatePomName = "alternate-pom.xml";
        File alternatePom = new File(testDir, alternatePomName);
        if (alternatePom.exists()) {
            alternatePom.delete();
        }
        Files.copy(pom.toPath(), alternatePom.toPath());
        // Now edit the pom.xml to trigger the dev mode restart
        filter(alternatePom, Collections.singletonMap("<!-- insert test dependencies here -->",
                "        <dependency>\n" +
                        "            <groupId>io.quarkus</groupId>\n" +
                        "            <artifactId>quarkus-smallrye-openapi</artifactId>\n" +
                        "        </dependency>"));

        runAndCheck();
        assertThat(DevModeTestUtils.getHttpResponse("/q/openapi", true)).contains("Resource not found");
        shutdownTheApp();

        runAndCheck("-f", alternatePomName);
        DevModeTestUtils.getHttpResponse("/q/openapi").contains("hello");
    }

    @Test
    public void testThatTheApplicationIsReloadedOnPomChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-pom-change");
        runAndCheck();

        // Edit the pom.xml.
        File source = new File(testDir, "pom.xml");
        filter(source, Collections.singletonMap("<!-- insert test dependencies here -->",
                "        <dependency>\n" +
                        "            <groupId>io.quarkus</groupId>\n" +
                        "            <artifactId>quarkus-smallrye-openapi</artifactId>\n" +
                        "        </dependency>"));

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/q/openapi").contains("hello"));
    }

    @Test
    public void testProjectWithExtension() throws MavenInvocationException, IOException {
        testDir = getTargetDir("projects/project-with-extension");
        runAndCheck();

        final List<String> artifacts = getNonReloadableArtifacts(
                Files.readAllLines(testDir.toPath().resolve("build-project-with-extension.log")));
        assertTrue(artifacts.contains("- org.acme:acme-quarkus-ext:1.0-SNAPSHOT"));
        assertTrue(artifacts.contains("- org.acme:acme-quarkus-ext-deployment:1.0-SNAPSHOT"));
        assertTrue(artifacts.contains("- org.acme:acme-common:1.0-SNAPSHOT"));
        assertTrue(artifacts.contains("- org.acme:acme-common-transitive:1.0-SNAPSHOT"));
        assertEquals(4, artifacts.size());
    }

    protected List<String> getNonReloadableArtifacts(final List<String> log) {
        final List<String> artifacts = new ArrayList<>();
        boolean inWarn = false;
        for (String line : log) {
            if (inWarn) {
                if (line.equals(
                        "The artifacts above appear to be either dependencies of non-reloadable application dependencies or Quarkus extensions")) {
                    break;
                }
                artifacts.add(line);
            } else if (line.equals(
                    "[WARNING] [io.quarkus.bootstrap.devmode.DependenciesFilter] Live reload was disabled for the following project artifacts:")) {
                inWarn = true;
            }
        }
        return artifacts;
    }

    @Test
    public void testRestClientCustomHeadersExtension() throws MavenInvocationException, IOException {
        testDir = getTargetDir("projects/rest-client-custom-headers-extension");
        runAndCheck();

        final List<String> artifacts = getNonReloadableArtifacts(
                Files.readAllLines(testDir.toPath().resolve("build-rest-client-custom-headers-extension.log")));
        assertTrue(artifacts.contains("- org.acme:rest-client-custom-headers:1.0-SNAPSHOT"));
        assertTrue(artifacts.contains("- org.acme:rest-client-custom-headers-deployment:1.0-SNAPSHOT"));
        assertEquals(2, artifacts.size());

        assertThat(DevModeTestUtils.getHttpResponse("/app/frontend")).isEqualTo("CustomValue1 CustomValue2");
    }

    @Test
    public void testThatTheApplicationIsReloadedMultiModule() throws MavenInvocationException, IOException {
        //we also check continuous testing
        testDir = initProject("projects/multimodule", "projects/multimodule-with-deps");
        runAndCheck();

        // test that we don't get multiple instances of a resource when loading from the ClassLoader
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/resourcesCount").equals("1"));

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils();
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();

        //check that the tests in both modules run
        Assertions.assertEquals(2, results.getTestsPassed());

        // Edit the "Hello" message.
        File source = new File(testDir, "rest/src/main/java/org/acme/HelloResource.java");
        final String uuid = UUID.randomUUID().toString();
        filter(source, Collections.singletonMap("return \"hello\";", "return \"" + uuid + "\";"));

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        results = testingTestUtils.waitForNextCompletion();

        //make sure the test is failing now
        Assertions.assertEquals(1, results.getTestsFailed());
        //now modify the passing test
        var testSource = new File(testDir, "rest/src/test/java/org/acme/test/SimpleTest.java");
        filter(testSource, Collections.singletonMap("Assertions.assertTrue(true);", "Assertions.assertTrue(false);"));
        results = testingTestUtils.waitForNextCompletion();
        Assertions.assertEquals(2, results.getTotalTestsFailed());
        //fix it again
        filter(testSource, Collections.singletonMap("Assertions.assertTrue(false);", "Assertions.assertTrue(true);"));
        results = testingTestUtils.waitForNextCompletion();
        Assertions.assertEquals(1, results.getTotalTestsFailed(), "Failed, actual results " + results);
        Assertions.assertEquals(1, results.getTotalTestsPassed(), "Failed, actual results " + results);

        filter(source, Collections.singletonMap(uuid, "carambar"));

        // Wait until we get "carambar"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("carambar"));

        // Create a new resource
        source = new File(testDir, "html/src/main/resources/META-INF/resources/lorem.txt");
        FileUtils.write(source,
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                "UTF-8");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/lorem.txt").contains("Lorem ipsum"));

        // Update the resource
        FileUtils.write(source, uuid, "UTF-8");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/lorem.txt").contains(uuid));

        // Delete the resource
        source.delete();
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/lorem.txt", 404));
    }

    @Test
    public void testMultiModuleDevModeWithLocalDepsDisabled() throws MavenInvocationException, IOException {
        testDir = initProject("projects/multimodule", "projects/multimodule-nodeps");
        runAndCheck("-DnoDeps");

        String greeting = DevModeTestUtils.getHttpResponse("/app/hello/greeting");
        assertThat(greeting).containsIgnoringCase("bonjour");

        // Edit the "Hello" message.
        File source = new File(testDir, "rest/src/main/java/org/acme/HelloResource.java");
        filter(source, Collections.singletonMap("return \"hello\";", "return \"" + UUID.randomUUID().toString() + "\";"));

        // Edit the greeting property.
        source = new File(testDir, "runner/src/main/resources/application.properties");
        final String uuid = UUID.randomUUID().toString();
        filter(source, Collections.singletonMap("greeting=bonjour", "greeting=" + uuid + ""));

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greeting").contains(uuid));

        greeting = DevModeTestUtils.getHttpResponse("/app/hello");
        assertThat(greeting).containsIgnoringCase("hello");
    }

    @Test
    public void testMultiModuleProjectWithRevisionVersion() throws MavenInvocationException, IOException {
        testDir = initProject("projects/multimodule-revision-prop");
        final String projectVersion = System.getProperty("project.version");
        runAndCheck("-Dquarkus.platform.version=" + projectVersion,
                "-Dquarkus-plugin.version=" + projectVersion);

        // Edit the "Hello" message.
        File source = new File(testDir, "rest/src/main/java/org/acme/HelloResource.java");
        final String uuid = UUID.randomUUID().toString();
        filter(source, Collections.singletonMap("return \"hello\";", "return \"" + uuid + "\";"));

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);
    }

    @Test
    public void testTestScopedLocalProjectDependency() throws MavenInvocationException, IOException {
        testDir = initProject("projects/test-module-dependency");
        final String projectVersion = System.getProperty("project.version");
        run(true, "-Dquarkus.platform.version=" + projectVersion,
                "-Dquarkus-plugin.version=" + projectVersion);

        assertEquals("Test class is not visible", DevModeTestUtils.getHttpResponse("/hello"));
    }

    @Test
    public void testThatTheApplicationIsReloadedOnNewResource() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-new-resource");
        runAndCheck();

        File source = new File(testDir, "src/main/java/org/acme/MyNewResource.java");
        String myNewResource = "package org.acme;\n" +
                "\n" +
                "import jakarta.ws.rs.GET;\n" +
                "import jakarta.ws.rs.Path;\n" +
                "import jakarta.ws.rs.Produces;\n" +
                "import jakarta.ws.rs.core.MediaType;\n" +
                "\n" +
                "@Path(\"/foo\")\n" +
                "public class MyNewResource {\n" +

                "    @GET\n" +
                "    @Produces(MediaType.TEXT_PLAIN)\n" +
                "    public String foo() {\n" +
                "        return \"bar\";\n" +
                "    }\n" +
                "}\n";
        FileUtils.write(source, myNewResource, StandardCharsets.UTF_8);

        // Wait until we get "bar"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/foo").contains("bar"));
    }

    @Test
    public void testThatClassFileAreCleanedUp() throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/classic", "projects/project-class-file-deletion");

        File source = new File(testDir, "src/main/java/org/acme/ClassDeletionResource.java");
        String classDeletionResource = "package org.acme;\n" +
                "\n" +
                "import jakarta.ws.rs.GET;\n" +
                "import jakarta.ws.rs.Path;\n" +
                "import jakarta.ws.rs.Produces;\n" +
                "import jakarta.ws.rs.core.MediaType;\n" +
                "\n" +
                "@Path(\"/deletion\")\n" +
                "public class ClassDeletionResource {\n" +
                "    public static class InnerClass {} \n" +
                "    @GET\n" +
                "    @Produces(MediaType.TEXT_PLAIN)\n" +
                "    public String toDelete() {\n" +
                "        return Hello.message();\n" +
                "    }\n" +
                "}\n " +
                "class Hello {\n" +
                "    public static String message() {\n" +
                "        return \"to be deleted\";\n" +
                "    }\n" +
                "}";
        FileUtils.write(source, classDeletionResource, StandardCharsets.UTF_8);

        runAndCheck();
        // Wait until source file is compiled
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/deletion").contains("to be deleted"));

        // Remove InnerClass
        filter(source, Collections.singletonMap("public static class InnerClass {}", ""));

        File helloClassFile = new File(testDir, "target/classes/org/acme/Hello.class");
        File innerClassFile = new File(testDir, "target/classes/org/acme/ClassDeletionResource$InnerClass.class");
        File classDeletionResourceClassFile = new File(testDir, "target/classes/org/acme/ClassDeletionResource.class");

        // Make sure that other class files have not been deleted.
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/package", 200));

        // Verify that only ClassDeletionResource$InnerClass.class to be deleted
        assertThat(innerClassFile).doesNotExist();
        assertThat(helloClassFile).exists();
        assertThat(classDeletionResourceClassFile).exists();

        // Delete source file
        source.delete();

        // Wait until we get "404 Not Found" because ClassDeletionResource.class have been deleted.
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/deletion", 404));

        // Make sure that class files for the deleted source file have also been deleted
        assertThat(helloClassFile).doesNotExist();
        assertThat(classDeletionResourceClassFile).doesNotExist();
    }

    @Test
    public void testSourceModificationBeforeFirstCallWorks() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-source-modification-before-first-call");
        run(true);

        File source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        // Edit the "Hello" message and provide a random string.
        String uuid = UUID.randomUUID().toString();
        filter(source, Collections.singletonMap("return \"hello\";", "return \"" + uuid + "\";"));

        // Check that the random string is returned
        String greeting = DevModeTestUtils.getHttpResponse("/app/hello");
        assertThat(greeting).containsIgnoringCase(uuid);
    }

    @Test
    public void testThatTheApplicationIsReloadedOnConfigChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-config-change");
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        final Properties mvnRunProps = new Properties();
        mvnRunProps.setProperty("debug", "false");
        running.execute(Arrays.asList("compile", "quarkus:dev", "-Dquarkus.analytics.disabled=true"), Collections.emptyMap(),
                mvnRunProps);

        String resp = DevModeTestUtils.getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = DevModeTestUtils.getHttpResponse("/app/hello/greeting");
        assertThat(greeting).containsIgnoringCase("bonjour");

        File source = new File(testDir, "src/main/resources/application.properties");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        String uuid = UUID.randomUUID().toString();
        filter(source, Collections.singletonMap("bonjour", uuid));

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greeting").contains(uuid));
    }

    @Test
    public void testThatAddingConfigFileWorksCorrectly() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-noconfig", "projects/project-classic-run-noconfig-add-config");
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        final Properties mvnRunProps = new Properties();
        mvnRunProps.setProperty("debug", "false");
        running.execute(Arrays.asList("compile", "quarkus:dev", "-Dquarkus.analytics.disabled=true"), Collections.emptyMap(),
                mvnRunProps);

        String resp = DevModeTestUtils.getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = DevModeTestUtils.getHttpResponse("/app/hello/greeting");
        assertThat(greeting).contains("initialValue");

        File configurationFile = new File(testDir, "src/main/resources/application.properties");
        assertThat(configurationFile).doesNotExist();

        String uuid = UUID.randomUUID().toString();

        FileUtils.write(configurationFile,
                "greeting=" + uuid,
                "UTF-8");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(configurationFile::isFile);

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greeting").contains(uuid));
    }

    @Test
    public void testThatExternalConfigOverridesConfigInJar() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-external-config");
        File configurationFile = new File(testDir, "config/application.properties");
        assertThat(configurationFile).doesNotExist();

        String uuid = UUID.randomUUID().toString();

        FileUtils.write(configurationFile,
                "greeting=" + uuid,
                "UTF-8");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(configurationFile::isFile);

        run(true);

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greeting").contains(uuid));
    }

    @Test
    public void testThatNewResourcesAreServed() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-resource-change");
        runAndCheck();

        // Create a new resource
        File source = new File(testDir, "src/main/resources/META-INF/resources/lorem.txt");
        FileUtils.write(source,
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                "UTF-8");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/lorem.txt"), containsString("Lorem ipsum"));

        // Update the resource
        String uuid = UUID.randomUUID().toString();
        FileUtils.write(source, uuid, "UTF-8");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/lorem.txt"), equalTo(uuid));

        // Delete the resource
        source.delete();
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/lorem.txt", 404));
    }

    @Test
    public void testThatConfigFileDeletionsAreDetected() throws MavenInvocationException, IOException {
        testDir = initProject("projects/dev-mode-file-deletion");
        runAndCheck();

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greetings").contains("Bonjour"));

        File source = new File(testDir, "src/main/resources/application.properties");
        FileUtils.delete(source);

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greetings").contains("Guten Morgen"));
    }

    @Test
    public void testThatMultipleResourceDirectoriesAreSupported() throws MavenInvocationException, IOException {
        testDir = initProject("projects/dev-mode-multiple-resource-dirs");
        testMultipleResourceDirectories();
    }

    @Test
    public void testThatMultipleResourceDirectoriesAreSupportedWithProfile() throws MavenInvocationException, IOException {
        testDir = initProject("projects/dev-mode-multiple-resource-dirs-with-profile");
        testMultipleResourceDirectories();
    }

    private void testMultipleResourceDirectories() throws MavenInvocationException, IOException {
        runAndCheck();
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greetings").contains("Bonjour/Other"));

        // Update the application.properties
        File source = new File(testDir, "src/main/resources-primary/application.properties");
        FileUtils.write(source, "greeting=Salut", "UTF-8");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greetings").contains("Salut/Other"));

        // Add the application.yaml
        source = new File(testDir, "src/main/resources-secondary/application.yaml");
        FileUtils.write(source, "other:\n" +
                "  greeting: Buenos dias", "UTF-8");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greetings").contains("Salut/Buenos dias"));

        // Update the application.yaml
        FileUtils.write(source, "other:\n" +
                "  greeting: Hola", "UTF-8");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/greetings").contains("Salut/Hola"));
    }

    @Test
    public void testThatApplicationRecoversCompilationIssue() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-compilation-issue");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        String uuid = UUID.randomUUID().toString();
        filter(source, Collections.singletonMap("return \"hello\";", "return \"" + uuid + "\"")); // No semi-colon

        // Wait until we get "uuid"
        AtomicReference<String> last = new AtomicReference<>();
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> {
                    String content = DevModeTestUtils.getHttpResponse("/app/hello", true);
                    last.set(content);
                    return content.contains(uuid);
                });

        assertThat(last.get()).containsIgnoringCase("error")
                .containsIgnoringCase("return \"" + uuid + "\"")
                .containsIgnoringCase("compile");

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);
        filter(source, Collections.singletonMap("\"" + uuid + "\"", "\"carambar\";"));

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("carambar"));
    }

    @Test
    public void testThatApplicationRecoversStartupIssue() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-classic-run-startup-issue");

        // Edit the JAX-RS resource to be package private
        File source = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        filter(source, Collections.singletonMap("public class HelloResource", "class HelloResource"));

        runAndExpectError();
        // Wait until we get the error page
        AtomicReference<String> last = new AtomicReference<>();
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> {
                    String content = DevModeTestUtils.getHttpResponse("/app/hello", true);
                    last.set(content);
                    return content.contains("Error restarting Quarkus");
                });

        assertThat(last.get()).containsIgnoringCase("Error restarting Quarkus");

        filter(source, Collections.singletonMap("class HelloResource", "public class HelloResource"));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> {
                    String content = DevModeTestUtils.getHttpResponse("/app/hello", true);
                    last.set(content);
                    return content.equals("hello");
                });
        assertThat(last.get()).isEqualTo("hello");
    }

    @Test
    public void testThatNewBeanAreDiscovered() throws IOException, MavenInvocationException {
        testDir = initProject("projects/classic", "projects/project-classic-run-new-bean");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(testDir, "src/main/java/org/acme/MyBean.java");
        String content = "package org.acme;\n" +
                "\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "\n" +
                "@ApplicationScoped\n" +
                "public class MyBean {\n" +
                "\n" +
                "    public String get() {\n" +
                "        return \"message\";\n" +
                "    }\n" +
                "    \n" +
                "}";
        FileUtils.write(source, content, "UTF-8");

        // Update the resource to use the bean
        File resource = new File(testDir, "src/main/java/org/acme/HelloResource.java");
        filter(resource, Collections.singletonMap("String greeting;", "String greeting;\n @Inject MyBean bean;"));
        filter(resource, Collections.singletonMap("\"hello\"", "bean.get()"));

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("message"));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        filter(source, Collections.singletonMap("message", "foobarbaz"));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("foobarbaz"));
    }

    @Test
    public void testNoErrorMessageWhenNoTarget() throws IOException, MavenInvocationException {
        testDir = initProject("projects/classic", "projects/project-no-target");
        FileUtils.deleteQuietly(new File(testDir, "target"));

        runAndCheck();
    }

    @Test
    public void testNoErrorMessageWhenNoTargetClasses() throws IOException, MavenInvocationException {
        testDir = initProject("projects/classic", "projects/project-no-classes");
        new File(testDir, "target").mkdirs();
        // Be sure we don't have classes.
        FileUtils.deleteQuietly(new File(testDir, "target/classes"));

        runAndCheck();
    }

    @Test
    public void testThatTheApplicationIsReloadedOnDotEnvConfigChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic", "projects/project-dotenv");
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        final Properties mvnRunProps = new Properties();
        mvnRunProps.setProperty("debug", "false");
        running.execute(Arrays.asList("compile", "quarkus:dev", "-Dquarkus.analytics.disabled=true"), Collections.emptyMap(),
                mvnRunProps);

        String resp = DevModeTestUtils.getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = DevModeTestUtils.getHttpResponse("/app/hello/otherGreeting");
        assertThat(greeting).containsIgnoringCase("Hola");

        File source = new File(testDir, ".env");
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        String uuid = UUID.randomUUID().toString();
        filter(source, Collections.singletonMap("Hola", uuid));

        // Wait until we get "uuid"
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello/otherGreeting").contains(uuid));
    }

    @Test
    public void testMultiModuleDevModeWithoutJavaSrc() throws MavenInvocationException, IOException {
        testDir = initProject("projects/multimodule", "projects/multimodule-no-java-src");
        runAndCheck();

        assertThat(running.log()).doesNotContain("The project's sources directory does not exist");
    }

    @Test
    public void testThatTheApplicationIsNotStartedWithoutBuildGoal() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-no-build");
        run(true);

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> running.log().contains("Skipping quarkus:dev as this is assumed to be a support library"));
    }

    @Test
    public void testThatTheApplicationIsStartedWithoutBuildGoalWhenNotEnforced() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-no-build", "projects/classic-no-build-not-enforced");
        runAndCheck("-Dquarkus.enforceBuildGoal=false");

        assertThat(running.log()).doesNotContain("Skipping quarkus:dev as this is assumed to be a support library");
    }

    @Test
    public void testResourcesFromClasspath() throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/multimodule-classpath", "projects/multimodule-resources-classpath");
        RunningInvoker invoker = new RunningInvoker(testDir, false);

        // to properly surface the problem of multiple classpath entries, we need to install the project to the local m2
        MavenProcessInvocationResult installInvocation = invoker.execute(
                List.of("clean", "install", "-DskipTests", "-Dquarkus.analytics.disabled=true"),
                Collections.emptyMap());
        assertThat(installInvocation.getProcess().waitFor(2, TimeUnit.MINUTES)).isTrue();
        assertThat(installInvocation.getExecutionException()).isNull();
        assertThat(installInvocation.getExitCode()).isEqualTo(0);

        // run dev mode from the runner module
        testDir = testDir.toPath().resolve("runner").toFile();
        run(true);

        // make sure the application starts
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> DevModeTestUtils.getHttpResponse("/cp/hello").equals("hello"));

        // test that we don't get multiple instances of a resource when loading from the ClassLoader
        assertThat(DevModeTestUtils.getHttpResponse("/cp/resourceCount/a.html")).isEqualTo("1");
        assertThat(DevModeTestUtils.getHttpResponse("/cp/resourceCount/entry")).isEqualTo("2");
    }

    @Test
    public void testThatDependencyInParentIsEvaluated() throws IOException, MavenInvocationException {
        testDir = initProject("projects/multimodule-parent-dep");
        runAndCheck();
    }

    @Test
    public void testModuleCompileOrder() throws IOException, MavenInvocationException {
        testDir = initProject("projects/multimodule-parent-dep", "projects/multimodule-compile-order");
        runAndCheck("-Dquarkus.bootstrap.effective-model-builder");

        assertThat(DevModeTestUtils.getHttpResponse("/app/hello/")).isEqualTo("hello");

        // modify classes in all the modules and make sure they are compiled in a correct order
        File resource = new File(testDir, "level0/src/main/java/org/acme/level0/Level0Service.java");
        filter(resource, Collections.singletonMap("getGreeting()", "getGreeting(String name)"));
        filter(resource, Collections.singletonMap("return greeting;", "return greeting + \" \" + name;"));

        resource = new File(testDir, "level1/src/main/java/org/acme/level1/Level1Service.java");
        filter(resource, Collections.singletonMap("getGreetingFromLevel0()", "getGreetingFromLevel0(String name)"));
        filter(resource, Collections.singletonMap("level0Service.getGreeting()", "level0Service.getGreeting(name)"));

        resource = new File(testDir, "level2/submodule/src/main/java/org/acme/level2/submodule/Level2Service.java");
        filter(resource, Collections.singletonMap("getGreetingFromLevel1()", "getGreetingFromLevel1(String name)"));
        filter(resource,
                Collections.singletonMap("level1Service.getGreetingFromLevel0()", "level1Service.getGreetingFromLevel0(name)"));

        resource = new File(testDir, "runner/src/main/java/org/acme/rest/HelloResource.java");
        filter(resource, Collections.singletonMap("level0Service.getGreeting()",
                "level0Service.getGreeting(\"world\")"));
        filter(resource, Collections.singletonMap("level2Service.getGreetingFromLevel1()",
                "level2Service.getGreetingFromLevel1(\"world\")"));

        await()
                .pollDelay(300, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("hello world"));
    }

    @Test
    public void testThatGenerateCodeGoalIsNotTriggeredIfNotConfigured() throws IOException, MavenInvocationException {
        testDir = initProject("projects/classic-no-generate");
        // the skip parameter triggers a log statement by the generate goal,
        // otherwise there would be no way to tell from the logfile that the goal was invoked
        runAndCheck(false, "-Dquarkus.generate-code.skip=true");

        assertThat(running.log()).doesNotContain("Skipping Quarkus code generation");
        assertThat(running.log()).contains("Copying 1 resource"); // maven-resource-plugin
        assertThat(running.log()).contains("Compiling 2 source files"); // maven-compiler-plugin
    }

    @Test
    public void testPropertyExpansion() throws IOException, MavenInvocationException {
        testDir = initProject("projects/property-expansion");
        runAndCheck();
        assertThat(DevModeTestUtils.getHttpResponse("/app/hello/")).isEqualTo("hello");
        assertThat(DevModeTestUtils.getHttpResponse("/app/hello/applicationName")).isEqualTo("myapp");
    }

    @Test
    public void testMultiJarModuleDevModeMocks() throws MavenInvocationException, IOException {
        testDir = initProject("projects/multijar-module", "projects/multijar-module-devmode-mocks");
        run(false, "clean", "package", "-DskipTests", "-Dqdev");

        String greeting = DevModeTestUtils.getHttpResponse("/hello");
        assertThat(greeting).contains("acme other mock-service");

        // Update TestBean
        File resource = new File(testDir, "beans/src/test/java/org/acme/testlib/mock/MockService.java");
        filter(resource, Collections.singletonMap("return \"mock-service\";", "return \"mock-service!\";"));
        await()
                .pollDelay(300, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("acme other mock-service!"));

        // Update AcmeBean
        resource = new File(testDir, "beans/src/main/java/org/acme/AcmeBean.java");
        filter(resource, Collections.singletonMap("return \"acme\";", "return \"acme!\";"));
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("acme! other mock-service!"));

        // Update Other bean
        resource = new File(testDir, "beans/src/main/java/org/acme/Other.java");
        filter(resource, Collections.singletonMap("return \"other\";", "return \"other!\";"));
        await()
                .pollDelay(300, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("acme! other! mock-service!"));
    }

    @Test
    public void testMultiJarModuleDevMode() throws MavenInvocationException, IOException {
        testDir = initProject("projects/multijar-module", "projects/multijar-module-devmode");
        run(false, "clean", "package", "-DskipTests");

        String greeting = DevModeTestUtils.getHttpResponse("/hello");
        assertThat(greeting).contains("acme other acme-service");

        // Update TestBean
        File resource = new File(testDir, "beans/src/main/java/org/acme/AcmeService.java");
        filter(resource, Collections.singletonMap("return \"acme-service\";", "return \"acme-service!\";"));
        await()
                .pollDelay(300, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("acme other acme-service!"));

        // Update AcmeBean
        resource = new File(testDir, "beans/src/main/java/org/acme/AcmeBean.java");
        filter(resource, Collections.singletonMap("return \"acme\";", "return \"acme!\";"));
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("acme! other acme-service!"));

        // Update Other bean
        resource = new File(testDir, "beans/src/main/java/org/acme/Other.java");
        filter(resource, Collections.singletonMap("return \"other\";", "return \"other!\";"));
        await()
                .pollDelay(300, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("acme! other! acme-service!"));
    }

    @Test
    public void testThatWatchedAbsolutePathsAreNotDeleted() throws MavenInvocationException, IOException {
        // for reference https://github.com/quarkusio/quarkus/issues/25667
        // .env files got deleted on dev mode restarts
        testDir = initProject("projects/no-resource-root", "projects/no-resource-root-run");
        run(true);

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("Servus"));

        // Update the .env
        File source = new File(testDir, ".env");
        FileUtils.write(source, "GREETING=Hallo", "UTF-8");

        assertTrue(source.exists());

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("Hallo"));

        assertTrue(source.exists());
    }

    @Test
    public void testExternalReloadableArtifacts() throws Exception {
        final String rootProjectPath = "projects/external-reloadable-artifacts";

        // Set up the external project
        final File externalJarDir = initProject(rootProjectPath + "/external-lib");

        // Clean and install the external JAR in local repository (.m2)
        install(externalJarDir, true);

        // Set up the main project that uses the external dependency
        this.testDir = initProject(rootProjectPath + "/app");

        // Run quarkus:dev process
        run(true);

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("Hello"));

        final File greetingJava = externalJarDir.toPath().resolve("src").resolve("main")
                .resolve("java").resolve("org").resolve("acme").resolve("lib")
                .resolve("Greeting.java").toFile();
        assertThat(greetingJava).exists();

        // Uncomment the method bonjour() in Greeting.java
        filter(greetingJava, Map.of("/*", "", "*/", ""));
        install(externalJarDir, false);

        final File greetingResourceJava = this.testDir.toPath().resolve("src").resolve("main")
                .resolve("java").resolve("org").resolve("acme")
                .resolve("GreetingResource.java").toFile();
        assertThat(greetingResourceJava).exists();

        // Update the GreetingResource.java to call the Greeting.bonjour() method
        final String greetingBonjourCall = "Greeting.bonjour()";
        filter(greetingResourceJava, Map.of("Greeting.hello()", greetingBonjourCall));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("Bonjour"));

        // Change bonjour() method content in Greeting.java
        filter(greetingJava, Map.of("Bonjour", "Bonjour!"));
        install(externalJarDir, false);

        // Change GreetingResource.java endpoint response to upper case letters
        filter(greetingResourceJava, Map.of(greetingBonjourCall, greetingBonjourCall.concat(".toUpperCase()")));

        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> DevModeTestUtils.getHttpResponse("/hello").contains("BONJOUR!"));
    }
}

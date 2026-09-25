package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationNativeTestFunctionalTest extends BaseGradleTest {

    private static final String NATIVE_BUILD_TASK = ":quarkusParityBuild";
    private static final String NATIVE_TEST_TASK = ":quarkusParityNativeTest";

    @Test
    void groovyDslRunsOwnedAndIncludedTestsWithConfigurationCacheReuse() throws IOException {
        writeGroovyApplication();
        writeNativeReceipt(javaLauncher());

        BuildResult firstResult = runNativeTests();

        assertTaskOutcomes(firstResult, SKIPPED, NATIVE_BUILD_TASK);
        assertTaskOutcomes(firstResult, SUCCESS, NATIVE_TEST_TASK);
        assertThat(firstResult.task(":integrationTest")).isNull();
        assertTestResults();

        BuildResult secondResult = runNativeTests();

        assertConfigurationCacheReused(secondResult);
        assertTaskOutcomes(secondResult, SKIPPED, NATIVE_BUILD_TASK);
        assertTaskOutcomes(secondResult, UP_TO_DATE, NATIVE_TEST_TASK);
        assertThat(secondResult.task(":integrationTest")).isNull();
        assertTestResults();
    }

    @Test
    void kotlinDslAcceptsTypedIncludedSuiteProvider() throws IOException {
        writeKotlinApplication();
        writeNativeReceipt(javaLauncher());

        BuildResult result = runNativeTests();

        assertTaskOutcomes(result, SKIPPED, NATIVE_BUILD_TASK);
        assertTaskOutcomes(result, SUCCESS, NATIVE_TEST_TASK);
        assertThat(result.task(":integrationTest")).isNull();
        assertTestResults();
    }

    @Test
    void executableContentChangeInvalidatesNativeTestAtTheSamePath() throws IOException {
        Assumptions.assumeFalse(isWindows(), "POSIX executable fixture");
        writeGroovyApplication();
        Path executable = testProjectDir.resolve("fake-native");
        writeExecutableScript(executable, "first");
        writeNativeReceipt(executable);

        BuildResult firstResult = runNativeTests();

        assertTaskOutcomes(firstResult, SKIPPED, NATIVE_BUILD_TASK);
        assertTaskOutcomes(firstResult, SUCCESS, NATIVE_TEST_TASK);

        Files.writeString(executable, "# changed executable content\n", StandardOpenOption.APPEND);
        BuildResult secondResult = runNativeTests();

        assertConfigurationCacheReused(secondResult);
        assertTaskOutcomes(secondResult, SKIPPED, NATIVE_BUILD_TASK);
        assertTaskOutcomes(secondResult, SUCCESS, NATIVE_TEST_TASK);
    }

    @Test
    void selectsEachNamedBuildsOwnReceiptWithoutSelectingTheOtherBuild() throws IOException {
        writeMultiNativeApplication();
        Path firstExecutable = createRegularFile("first-native");
        Path secondExecutable = createRegularFile("second-native");
        writeNativeReceipt("first", firstExecutable);
        writeNativeReceipt("second", secondExecutable);

        BuildResult firstResult = buildResultWithIsolatedProjects(":quarkusFirstNativeTest", BUILD_CACHE);

        assertTaskOutcomes(firstResult, SKIPPED, ":quarkusFirstBuild");
        assertTaskOutcomes(firstResult, SUCCESS, ":quarkusFirstNativeTest");
        assertThat(firstResult.task(":quarkusSecondBuild")).isNull();
        assertThat(firstResult.task(":quarkusSecondNativeTest")).isNull();
        assertThat(testProjectDir.resolve(
                "build/test-results/quarkusFirstNativeTest/TEST-org.acme.FirstNativeTest.xml"))
                .isRegularFile();

        BuildResult secondResult = buildResultWithIsolatedProjects(":quarkusSecondNativeTest", BUILD_CACHE);

        assertTaskOutcomes(secondResult, SKIPPED, ":quarkusSecondBuild");
        assertTaskOutcomes(secondResult, SUCCESS, ":quarkusSecondNativeTest");
        assertThat(secondResult.task(":quarkusFirstBuild")).isNull();
        assertThat(secondResult.task(":quarkusFirstNativeTest")).isNull();
        assertThat(testProjectDir.resolve(
                "build/test-results/quarkusSecondNativeTest/TEST-org.acme.SecondNativeTest.xml"))
                .isRegularFile();
    }

    @Test
    void supportsGradleTestFilteringAcrossOwnedAndIncludedTests() throws IOException {
        writeGroovyApplication();
        writeNativeReceipt(javaLauncher());

        BuildResult result = buildResultWithIsolatedProjects(
                NATIVE_TEST_TASK, "--tests", "org.acme.IncludedNativeTest", BUILD_CACHE);

        assertTaskOutcomes(result, SKIPPED, NATIVE_BUILD_TASK);
        assertTaskOutcomes(result, SUCCESS, NATIVE_TEST_TASK);
        Path resultDirectory = testProjectDir.resolve("build/test-results/quarkusParityNativeTest");
        assertThat(resultDirectory.resolve("TEST-org.acme.IncludedNativeTest.xml")).isRegularFile();
        assertThat(resultDirectory.resolve("TEST-org.acme.OwnedNativeTest.xml")).doesNotExist();
    }

    @Test
    void nativeBuildFailurePreventsNativeTestExecution() throws IOException {
        writeGroovyApplication("""
                tasks.named('quarkusParityBuild') {
                    enabled = true
                    doFirst {
                        throw new GradleException('synthetic native build failure')
                    }
                }
                """);

        BuildResult result = prepareBuildWithIsolatedProjects(NATIVE_TEST_TASK, BUILD_CACHE).buildAndFail();

        assertTaskOutcomes(result, FAILED, NATIVE_BUILD_TASK);
        assertThat(result.task(NATIVE_TEST_TASK)).isNull();
        assertThat(result.getOutput()).contains("synthetic native build failure");
        assertThat(testProjectDir.resolve("build/test-results/quarkusParityNativeTest")).doesNotExist();
    }

    @Test
    void malformedNativeReceiptFailsThroughTheNativeTestTaskWithActionableContext() throws IOException {
        writeGroovyApplication();
        Path receipt = nativeReceipt("parity");
        Files.createDirectories(receipt.getParent());
        Files.writeString(receipt, "not-a-native-result");

        BuildResult result = prepareBuildWithIsolatedProjects(NATIVE_TEST_TASK, BUILD_CACHE).buildAndFail();

        assertThat(result.getOutput())
                .contains("Quarkus integration-test suite 'quarkusParityNativeTest'")
                .contains("could not read the native result for build 'parity'")
                .contains(canonicalPath(receipt).toString());
        assertThat(testProjectDir.resolve("build/test-results/quarkusParityNativeTest")).doesNotExist();
    }

    @Test
    void isolatedMultiprojectBuildRejectsForeignSuiteProviderWithoutRealizingIt() throws IOException {
        writeForeignSuiteMultiproject("foreignTest", false);

        BuildResult result = prepareBuildWithIsolatedProjects(":zApp:help", BUILD_CACHE).buildAndFail();

        assertThat(result.getOutput())
                .contains("Quarkus named native-test suite 'quarkusParityNativeTest'")
                .contains("cannot include JVM test suite 'foreignTest'")
                .contains("owned by project ':zApp'");
        assertThat(testProjectDir.resolve("aForeign/foreign-suite-realized")).doesNotExist();
    }

    @Test
    void isolatedMultiprojectBuildResolvesForeignProviderNameToLocalSuiteWithoutRealizingIt() throws IOException {
        writeForeignSuiteMultiproject("integrationTest", true);

        BuildResult firstResult = buildResultWithIsolatedProjects(":zApp:help", BUILD_CACHE);

        assertThat(firstResult.getOutput()).contains("BUILD SUCCESSFUL");
        assertThat(testProjectDir.resolve("aForeign/foreign-suite-realized")).doesNotExist();

        BuildResult secondResult = buildResultWithIsolatedProjects(":zApp:help", BUILD_CACHE);

        assertConfigurationCacheReused(secondResult);
        assertThat(testProjectDir.resolve("aForeign/foreign-suite-realized")).doesNotExist();
    }

    private BuildResult runNativeTests() {
        return buildResultWithIsolatedProjects(NATIVE_TEST_TASK, BUILD_CACHE);
    }

    private void writeGroovyApplication() throws IOException {
        writeGroovyApplication("");
    }

    private void writeGroovyApplication(String additionalBuildScript) throws IOException {
        writeCommonApplicationFiles();
        writeFile("build.gradle", """
                import org.gradle.api.plugins.jvm.JvmTestSuite

                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.3'
                    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.3'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher:1.10.3'
                    testRuntimeOnly 'org.jboss.logmanager:jboss-logmanager:3.2.2.Final'
                }

                def integrationTest = testing.suites.register('integrationTest', JvmTestSuite) {
                    dependencies {
                        implementation 'org.junit.jupiter:junit-jupiter-api:5.10.3'
                        runtimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.3'
                        runtimeOnly 'org.junit.platform:junit-platform-launcher:1.10.3'
                    }
                }

                quarkusApplication {
                    builds {
                        nativeExecutable('parity')
                    }
                }

                testing {
                    suites {
                        named('quarkusParityNativeTest', JvmTestSuite) {
                            includeTestsFrom integrationTest
                        }
                    }
                }

                tasks.named('quarkusParityBuild') {
                    enabled = false
                }
                """ + additionalBuildScript);
    }

    private void writeKotlinApplication() throws IOException {
        writeCommonApplicationFiles();
        writeFile("build.gradle.kts", """
                import org.gradle.api.plugins.jvm.JvmTestSuite
                import org.gradle.kotlin.dsl.dependencies
                import org.gradle.kotlin.dsl.named
                import org.gradle.kotlin.dsl.register

                plugins {
                    id("io.quarkus.application")
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
                    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
                    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
                    testRuntimeOnly("org.jboss.logmanager:jboss-logmanager:3.2.2.Final")
                }

                val integrationTest = testing.suites.register<JvmTestSuite>("integrationTest") {
                    dependencies {
                        implementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
                        runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
                        runtimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
                    }
                }

                quarkusApplication {
                    builds {
                        nativeExecutable("parity")
                    }
                }

                testing.suites.named<JvmTestSuite>("quarkusParityNativeTest") {
                    includeTestsFrom(integrationTest)
                }

                tasks.named("quarkusParityBuild") {
                    enabled = false
                }
                """);
    }

    private void writeCommonApplicationFiles() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'mocked-native-test'\n");
        writeFile("gradle.properties", "version = 1.0\n");
        writeFile("src/main/java/org/acme/Application.java", """
                package org.acme;

                public final class Application {
                }
                """);
        writeFile("src/quarkusParityNativeTest/java/org/acme/OwnedNativeTest.java", """
                package org.acme;

                import static java.nio.charset.StandardCharsets.UTF_8;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                import static org.junit.jupiter.api.Assertions.assertFalse;
                import static org.junit.jupiter.api.Assertions.assertNotNull;
                import static org.junit.jupiter.api.Assertions.assertTrue;

                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.util.concurrent.TimeUnit;

                import org.junit.jupiter.api.Test;

                class OwnedNativeTest {

                    @Test
                    void receivesAndCanInvokeSelectedNativeExecutable() throws Exception {
                        String nativeImage = System.getProperty("native.image.path");
                        assertNotNull(nativeImage);
                        assertFalse(nativeImage.isBlank());
                        assertTrue(Files.isRegularFile(Path.of(nativeImage)), nativeImage);

                        Process process = new ProcessBuilder(nativeImage, "-version")
                                .redirectErrorStream(true)
                                .start();
                        try {
                            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "native executable timed out");
                            String output = new String(process.getInputStream().readAllBytes(), UTF_8);
                            assertEquals(0, process.exitValue(), output);
                        } finally {
                            if (process.isAlive()) {
                                process.destroy();
                                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                                    process.destroyForcibly();
                                    assertTrue(process.waitFor(5, TimeUnit.SECONDS),
                                            "native executable did not terminate");
                                }
                            }
                        }
                    }
                }
                """);
        writeFile("src/integrationTest/java/org/acme/IncludedNativeTest.java", """
                package org.acme;

                import static org.junit.jupiter.api.Assertions.assertNotNull;

                import org.junit.jupiter.api.Test;

                class IncludedNativeTest {

                    @Test
                    void includedSuiteReceivesSelectedNativeExecutable() {
                        assertNotNull(System.getProperty("native.image.path"));
                    }
                }
                """);
    }

    private void assertTestResults() {
        Path resultDirectory = testProjectDir.resolve("build/test-results/quarkusParityNativeTest");
        assertThat(resultDirectory.resolve("TEST-org.acme.OwnedNativeTest.xml")).isRegularFile();
        assertThat(resultDirectory.resolve("TEST-org.acme.IncludedNativeTest.xml")).isRegularFile();
    }

    private void writeNativeReceipt(Path executable) throws IOException {
        writeNativeReceipt("parity", executable);
    }

    private void writeNativeReceipt(String buildName, Path executable) throws IOException {
        Path receipt = nativeReceipt(buildName);
        Files.createDirectories(receipt.getParent());
        Properties properties = new Properties();
        properties.setProperty("schema.version", "1");
        properties.setProperty("result.type", "native-executable");
        properties.setProperty("build.name", buildName);
        properties.setProperty("native.output-root", executable.getParent().toAbsolutePath().toString());
        properties.setProperty("native.output-name", executable.getFileName().toString());
        properties.setProperty("native.executable.path", executable.toAbsolutePath().toString());
        properties.setProperty("native.artifact.count", "0");
        try (OutputStream output = Files.newOutputStream(receipt)) {
            properties.store(output, null);
        }
    }

    private Path nativeReceipt(String buildName) {
        return testProjectDir.resolve(
                "build/quarkus-build-results/" + buildName + "/package/native-result.properties");
    }

    private Path createRegularFile(String fileName) throws IOException {
        Path file = testProjectDir.resolve(fileName);
        Files.writeString(file, fileName);
        return file;
    }

    private void writeMultiNativeApplication() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'multiple-mocked-native-tests'\n");
        writeFile("gradle.properties", "version = 1.0\n");
        writeFile("build.gradle", """
                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.3'
                    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.3'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher:1.10.3'
                    testRuntimeOnly 'org.jboss.logmanager:jboss-logmanager:3.2.2.Final'
                }

                quarkusApplication {
                    builds {
                        nativeExecutable('first')
                        nativeExecutable('second')
                    }
                }

                tasks.named('quarkusFirstBuild') {
                    enabled = false
                }
                tasks.named('quarkusSecondBuild') {
                    enabled = false
                }
                """);
        writeFile("src/main/java/org/acme/Application.java", """
                package org.acme;

                public final class Application {
                }
                """);
        writeNativePathTest("First", "first-native");
        writeNativePathTest("Second", "second-native");
    }

    private void writeNativePathTest(String name, String expectedExecutableName) throws IOException {
        writeFile("src/quarkus" + name + "NativeTest/java/org/acme/" + name + "NativeTest.java", """
                package org.acme;

                import static org.junit.jupiter.api.Assertions.assertTrue;

                import org.junit.jupiter.api.Test;

                class %1$sNativeTest {

                    @Test
                    void selectsMatchingNativeResult() {
                        String nativeImage = System.getProperty("native.image.path");
                        assertTrue(nativeImage.endsWith("%2$s"), nativeImage);
                    }
                }
                """.formatted(name, expectedExecutableName));
    }

    private void writeForeignSuiteMultiproject(String foreignSuiteName, boolean registerLocalSuite) throws IOException {
        writeFile("settings.gradle", """
                rootProject.name = 'foreign-suite-provider'
                include 'aForeign', 'zApp'
                """);
        writeFile("buildSrc/src/main/java/org/acme/ForeignSuiteHolder.java", """
                package org.acme;

                import java.io.IOException;
                import java.io.UncheckedIOException;
                import java.lang.reflect.InvocationTargetException;
                import java.lang.reflect.Proxy;
                import java.nio.file.Files;
                import java.nio.file.Path;

                import org.gradle.api.NamedDomainObjectProvider;
                import org.gradle.api.plugins.jvm.JvmTestSuite;

                public final class ForeignSuiteHolder {

                    public static NamedDomainObjectProvider<JvmTestSuite> suite;

                    private ForeignSuiteHolder() {
                    }

                    @SuppressWarnings("unchecked")
                    public static NamedDomainObjectProvider<JvmTestSuite> trackRealization(
                            NamedDomainObjectProvider<JvmTestSuite> delegate, Path marker) {
                        return (NamedDomainObjectProvider<JvmTestSuite>) Proxy.newProxyInstance(
                                ForeignSuiteHolder.class.getClassLoader(),
                                new Class<?>[] { NamedDomainObjectProvider.class },
                                (proxy, method, arguments) -> {
                                    if (method.getName().equals("get") && method.getParameterCount() == 0) {
                                        try {
                                            Files.writeString(marker, "realized");
                                        } catch (IOException e) {
                                            throw new UncheckedIOException(e);
                                        }
                                    }
                                    try {
                                        return method.invoke(delegate, arguments);
                                    } catch (InvocationTargetException e) {
                                        throw e.getCause();
                                    }
                                });
                    }
                }
                """);
        writeFile("aForeign/build.gradle", """
                import org.acme.ForeignSuiteHolder
                import org.gradle.api.plugins.jvm.JvmTestSuite

                plugins {
                    id 'jvm-test-suite'
                }

                def foreignSuite = testing.suites.register('%s', JvmTestSuite)
                ForeignSuiteHolder.suite = ForeignSuiteHolder.trackRealization(
                    foreignSuite, file('foreign-suite-realized').toPath())
                """.formatted(foreignSuiteName));
        writeFile("zApp/build.gradle", """
                import org.acme.ForeignSuiteHolder
                import org.gradle.api.plugins.jvm.JvmTestSuite

                plugins {
                    id 'io.quarkus.application'
                }

                %s

                quarkusApplication {
                    builds {
                        nativeExecutable('parity')
                    }
                }

                testing.suites.named('quarkusParityNativeTest', JvmTestSuite) {
                    includeTestsFrom ForeignSuiteHolder.suite
                }
                """.formatted(registerLocalSuite
                ? "def integrationTest = testing.suites.register('integrationTest', JvmTestSuite)"
                : ""));
    }

    private void writeExecutableScript(Path executable, String value) throws IOException {
        writeFile(executable, """
                #!/bin/sh
                echo '%s'
                """.formatted(value));
        Files.setPosixFilePermissions(executable, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE));
    }

    private static Path javaLauncher() {
        return Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }
}

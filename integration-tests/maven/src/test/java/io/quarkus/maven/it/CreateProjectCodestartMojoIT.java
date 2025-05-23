package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.platform.tools.ToolsConstants;

@DisableForNative
public class CreateProjectCodestartMojoIT extends QuarkusPlatformAwareMojoTestBase {

    private static final Logger LOG = Logger.getLogger(CreateProjectCodestartMojoIT.class.getName());

    private File testDir;

    private static Stream<Arguments> provideLanguages() {
        return Stream.of("java", "kotlin")
                .flatMap(l -> Stream.of("", "rest", "qute").map(e -> Arguments.of(l, e)));
    }

    @Test
    public void testCodestartOutsideCatalog() throws Exception {
        testDir = initProject("projects/extension-codestart");
        final Invoker invoker = initInvoker(testDir);

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(List.of("install"));
        request.setDebug(false);
        request.setShowErrors(true);
        File log = new File(testDir, "install-extension-" + testDir.getName() + ".log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(log), false, "UTF-8"),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);
        invoker.execute(request);

        final Path generatedProjectPath = generateProject("maven", "java", "org.acme.quarkus:acme-quarkus:1.0.0-SNAPSHOT",
                Map.of());
        final Path codestartClass = generatedProjectPath.resolve("src/main/java/org/test/CustomCode.java");
        assertThat(codestartClass).exists();
    }

    @ParameterizedTest
    @MethodSource("provideLanguages")
    public void generateMavenProject(String language, String extensions) throws Exception {
        final Path generatedProjectPath = generateProject("maven", language, extensions, Collections.emptyMap());
        checkDir(generatedProjectPath.resolve("src/main/" + language));
        Stream.of(extensions.split(","))
                .filter(s -> !s.isEmpty())
                .forEach(e -> checkContent(generatedProjectPath.resolve("pom.xml"), e));
    }

    @ParameterizedTest
    @MethodSource("provideLanguages")
    public void generateGradleProject(String language, String extensions) throws Exception {
        final Path generatedProjectPath = generateProject("gradle", language, extensions, Collections.emptyMap());
        checkDir(generatedProjectPath.resolve("src/main/" + language));
        Stream.of(extensions.split(","))
                .forEach(e -> checkContent(generatedProjectPath.resolve("build.gradle"), e));
    }

    @ParameterizedTest
    @MethodSource("provideLanguages")
    public void generateGradleKotlinProject(String language, String extensions) throws Exception {
        final Path generatedProjectPath = generateProject("gradle-kotlin-dsl", language, extensions, Collections.emptyMap());
        checkDir(generatedProjectPath.resolve("src/main/" + language));
        Stream.of(extensions.split(","))
                .forEach(e -> checkContent(generatedProjectPath.resolve("build.gradle.kts"), e));
    }

    @Test
    public void generateCustomRESTJavaProject() throws Exception {
        final HashMap<String, String> options = new HashMap<>();
        options.put("path", "/bonjour");
        options.put("className", "com.andy.BonjourResource");
        final Path generatedProjectPath = generateProject("maven", "java",
                "rest", options);
        checkDir(generatedProjectPath.resolve("src/main/java/com/andy"));
        checkContent(generatedProjectPath.resolve("src/main/java/com/andy/BonjourResource.java"),
                "package com.andy;",
                "class BonjourResource",
                "@Path(\"/bonjour\")");

        checkContent(generatedProjectPath.resolve("src/test/java/com/andy/BonjourResourceTest.java"),
                "package com.andy;",
                "class BonjourResourceTest",
                "\"/bonjour\"");

        checkContent(generatedProjectPath.resolve("src/test/java/com/andy/BonjourResourceIT.java"),
                "package com.andy;",
                "class BonjourResourceIT extends BonjourResourceTest");
    }

    private Path generateProject(String buildtool, String language, String extensions, Map<String, String> options)
            throws Exception {
        final StringBuilder name = new StringBuilder();
        name.append("project-").append(buildtool).append('-').append(language);
        if (extensions.isEmpty()) {
            name.append("-commandmode");
        } else {
            name.append('-');
            for (int i = 0; i < extensions.length(); ++i) {
                char c = extensions.charAt(i);
                if (c == ',') {
                    c = '-';
                } else if (c == ':') {
                    c = '-';
                }
                name.append(c);
            }
        }
        if (!options.isEmpty()) {
            name.append("-custom");
        }
        testDir = prepareTestDir(name.toString());
        LOG.info("creating project in " + testDir.toPath().toString());
        return runCreateCommand(buildtool, extensions + (!Objects.equals(language, "java") ? "," + language : ""), options);
    }

    private static File prepareTestDir(String name) {
        File tc = new File("target/codestart-test/" + name);
        if (tc.isDirectory()) {
            try {
                FileUtils.deleteDirectory(tc);
            } catch (IOException e) {
                throw new RuntimeException("Cannot delete directory: " + tc, e);
            }
        }
        boolean mkdirs = tc.mkdirs();
        LOG.log(Level.FINE, "codestart-test created? %s", mkdirs);
        return tc;
    }

    private Path runCreateCommand(String buildTool, String extensions, Map<String, String> options)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {
        // Scaffold the new project
        assertThat(testDir).isDirectory();

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.test");
        properties.put("projectArtifactId", "my-test-app");
        properties.put("codestartsEnabled", "true");
        properties.put("buildTool", buildTool);
        properties.put("extensions", extensions);
        properties.putAll(options);

        InvocationResult result = executeCreate(properties);

        assertThat(result.getExitCode()).isZero();

        return testDir.toPath().resolve("my-test-app");
    }

    private InvocationResult executeCreate(Properties params)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {
        Invoker invoker = initInvoker(testDir);
        params.setProperty("platformGroupId", ToolsConstants.IO_QUARKUS);
        params.setProperty("platformArtifactId", "quarkus-bom");
        params.setProperty("platformVersion", getQuarkusCoreVersion());

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList(
                getMavenPluginGroupId() + ":" + getMavenPluginArtifactId() + ":" + getMavenPluginVersion() + ":create"));
        request.setDebug(false);
        request.setShowErrors(true);
        request.setProperties(params);

        PrintStreamLogger logger = getPrintStreamLogger("create-codestart.log");
        invoker.setLogger(logger);
        return invoker.execute(request);
    }

    private PrintStreamLogger getPrintStreamLogger(String s) throws UnsupportedEncodingException, FileNotFoundException {
        File log = new File(testDir, s);
        return new PrintStreamLogger(new PrintStream(new FileOutputStream(log), false, "UTF-8"),
                InvokerLogger.DEBUG);
    }

    private void checkContent(final Path resource, final String... contentsToFind) {
        assertThat(resource).isRegularFile();
        Stream.of(contentsToFind)
                .forEach(c -> {
                    try {
                        assertThat(FileUtils.readFileToString(resource.toFile(), "UTF-8")).contains(c);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private void checkDir(final Path dir) throws IOException {
        assertThat(dir).isDirectory();
    }
}

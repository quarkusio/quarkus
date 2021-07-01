package io.quarkus.devtools.codestarts.quarkus;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.Sets;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartProjectDefinition;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Tag;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.testing.PlatformAwareTestBase;
import io.quarkus.devtools.testing.SnapshotTesting;
import io.quarkus.devtools.testing.WrapperRunner;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTesting;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuarkusCodestartBuildIT extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/quarkus-codestart-build-test");

    private static final Set<String> EXCLUDED = Sets.newHashSet(
            "azure-functions-http-example");

    private static final Set<String> RUN_ALONE = Sets.newHashSet("resteasy-reactive-codestart", "picocli-codestart");

    @BeforeAll
    static void setUp() throws IOException {
        SnapshotTesting.deleteTestDirectory(testDirPath.toFile());
    }

    private Map<String, Object> getTestInputData(final Map<String, Object> override) {
        return QuarkusCodestartTesting.getRealTestInputData(getExtensionsCatalog(), override);
    }

    @Test
    public void testRunTogetherCodestartsJava() throws Exception {
        generateProjectRunTests("maven", "java", getExtensionCodestarts());
    }

    @Test
    public void testRunTogetherCodestartsKotlin() throws Exception {
        generateProjectRunTests("maven", "kotlin", getExtensionCodestarts());
    }

    @Test
    public void testRunTogetherCodestartsScala() throws Exception {
        generateProjectRunTests("maven", "scala", getExtensionCodestarts());
    }

    @ParameterizedTest
    @MethodSource("provideLanguages")
    @org.junit.jupiter.api.Tag("failsOnJDK16")
    public void testGradle(String language) throws Exception {
        final List<String> codestarts = getExtensionCodestarts();
        generateProjectRunTests("gradle", language, codestarts);
    }

    @ParameterizedTest
    @MethodSource("provideLanguages")
    @org.junit.jupiter.api.Tag("failsOnJDK16")
    public void testGradleKotlinDSL(String language) throws Exception {
        final List<String> codestarts = getExtensionCodestarts();
        generateProjectRunTests("gradle-kotlin-dsl", language, codestarts);
    }

    @ParameterizedTest
    @MethodSource("provideRunAloneCodestarts")
    public void testRunAloneCodestartsJava(String codestart) throws Exception {
        generateProjectRunTests("maven", "java", singletonList(codestart));
    }

    @ParameterizedTest
    @MethodSource("provideRunAloneCodestarts")
    public void testRunAloneCodestartsKotlin(String codestart) throws Exception {
        generateProjectRunTests("maven", "kotlin", singletonList(codestart));
    }

    @ParameterizedTest
    @MethodSource("provideRunAloneCodestarts")
    public void testRunAloneCodestartsScala(String codestart) throws Exception {
        generateProjectRunTests("maven", "scala", singletonList(codestart));
    }

    @Test
    public void generateAzureFunctionsHttpExampleProjectRun() throws Exception {
        generateProjectRunTests("maven", "java", singletonList("azure-functions-http-example"));
    }

    private void generateProjectRunTests(String buildTool, String language, List<String> codestarts)
            throws Exception {
        generateProjectRunTests(buildTool, language, codestarts, genName(buildTool, language, codestarts));
    }

    private void generateProjectRunTests(String buildToolName, String language, List<String> codestarts, String name)
            throws Exception {
        final BuildTool buildTool = BuildTool.findTool(buildToolName);

        final Map<String, Object> data = getTestInputData(Collections.singletonMap("artifact-id", name));
        // for JVM 8 and 14 this will generate project with java 1.8, for JVM 11 project with java 11
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(data)
                .buildTool(buildTool)
                .addCodestarts(codestarts)
                .addCodestart(language)
                .addBoms(QuarkusCodestartTesting.getBoms(data))
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        Path projectDir = testDirPath.resolve(name);
        projectDefinition.generate(projectDir);

        final int result = WrapperRunner.run(projectDir,
                WrapperRunner.Wrapper.fromBuildtool(buildToolName));
        assertThat(result).isZero();
    }

    private String genName(String buildtool, String language, List<String> codestarts) {
        String name = "project-" + buildtool + "-" + language;
        if (codestarts.isEmpty()) {
            name += "-default";
        } else if (codestarts.size() > 2) {
            name += "-" + UUID.randomUUID().toString();
        } else {
            name += "-" + String.join("-", codestarts);
        }
        return name;
    }

    private QuarkusCodestartCatalog getCatalog() throws IOException {
        return QuarkusCodestartCatalog.fromExtensionsCatalog(getExtensionsCatalog(), getCodestartsResourceLoaders());
    }

    private List<String> getExtensionCodestarts() throws IOException {
        return getAllExamples()
                .filter(c -> !shouldRunAlone(c))
                .map(Codestart::getName)
                .collect(Collectors.toList());
    }

    private Stream<Arguments> provideRunAloneCodestarts() throws IOException {
        return getAllExamples()
                .filter(this::shouldRunAlone)
                .map(Codestart::getName)
                .map(Arguments::of);
    }

    private Stream<Arguments> provideLanguages() {
        return Stream.of("java", "kotlin", "scala")
                .map(Arguments::of);
    }

    private Stream<Codestart> getAllExamples() throws IOException {
        return getCatalog().getCodestarts().stream()
                .filter(QuarkusCodestartCatalog::isExtensionCodestart)
                .filter(c -> !EXCLUDED.contains(c.getName()));
    }

    private boolean shouldRunAlone(Codestart c) {
        return c.containsTag(Tag.EXAMPLE.key()) || RUN_ALONE.contains(c.getName());
    }
}

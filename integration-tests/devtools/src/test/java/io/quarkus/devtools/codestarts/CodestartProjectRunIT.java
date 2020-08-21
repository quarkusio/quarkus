package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.QuarkusCodestarts.inputBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.Sets;

import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodestartProjectRunIT extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/codestarts-run-test");

    private static final Set<String> RUN_MANUALLY_CODESTARTS = Sets.newHashSet(
            "azure-functions-http-example");
    private static final Set<String> RUN_ALONE_CODESTARTS = Sets.newHashSet(
            "funqy-amazon-lambda-example",
            "funqy-knative-events-example",
            "amazon-lambda-example");

    @BeforeAll
    static void setUp() throws IOException {
        ProjectTestUtil.delete(testDirPath.toFile());
    }

    private Map<String, Object> getTestInputData() {
        return getTestInputData(null);
    }

    private Map<String, Object> getTestInputData(final Map<String, Object> override) {
        return CodestartProjectGenerationIT.getTestInputData(getPlatformDescriptor(), override);
    }

    private Stream<Arguments> provideGenerateCombinations() throws IOException {
        final List<String> examples = CodestartLoader
                .loadCodestartsFromExtensions(QuarkusCodestarts.resourceLoader(getPlatformDescriptor())).stream()
                .filter(c -> c.getSpec().isExample())
                .map(Codestart::getName)
                .filter(c -> !RUN_MANUALLY_CODESTARTS.contains(c))
                .collect(Collectors.toList());
        final List<List<String>> runAlone = examples.stream().filter(RUN_ALONE_CODESTARTS::contains)
                .map(Collections::singletonList).collect(Collectors.toList());
        final List<String> runTogether = examples.stream().filter(o -> !RUN_ALONE_CODESTARTS.contains(o))
                .collect(Collectors.toList());
        return Stream.of("java", "kotlin", "scala")
                .flatMap(l -> Stream.concat(Stream.of(runTogether, Collections.emptyList()), Stream.of(runAlone.toArray()))
                        .map(c -> Arguments.of(l, c)));
    }

    @ParameterizedTest
    @MethodSource("provideGenerateCombinations")
    public void generateMavenProjectRun(String language, List<String> codestarts) throws Exception {
        generateProjectRunTests("maven", language, codestarts);
    }

    @ParameterizedTest
    @MethodSource("provideGenerateCombinations")
    public void generateGradleProjectRun(String language, List<String> codestarts) throws Exception {
        generateProjectRunTests("gradle", language, codestarts);
    }

    @ParameterizedTest
    @MethodSource("provideGenerateCombinations")
    public void generateGradleKotlinProjectRun(String language, List<String> codestarts) throws Exception {
        generateProjectRunTests("gradle-kotlin-dsl", language, codestarts);
    }

    @Test
    public void generateAzureFunctionsHttpExampleProjectRun() throws Exception {
        generateProjectRunTests("maven", "java", Collections.singletonList("azure-functions-http-example"));
    }

    private void generateProjectRunTests(String buildtool, String language, List<String> codestarts) throws Exception {
        String name = "project-" + buildtool + "-" + language;
        if (codestarts.isEmpty()) {
            name += "-commandmode";
        } else {
            name += "-" + String.join("-", codestarts);
        }
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .includeExamples()
                .addData(getTestInputData(Collections.singletonMap("artifact-id", name)))
                .addCodestarts(codestarts)
                .addCodestart(buildtool)
                // for JVM 8 and 14 this will generate project with java 1.8, for JVM 11 project with java 11
                .putData("java.version", System.getProperty("java.specification.version"))
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        Path projectDir = testDirPath.resolve(name);
        Codestarts.generateProject(codestartProject, projectDir);
        final int result = CodestartProjectTestRunner.run(projectDir,
                CodestartProjectTestRunner.Wrapper.fromBuildtool(buildtool));
        assertThat(result).isZero();
    }
}
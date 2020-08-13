package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.QuarkusCodestarts.inputBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodestartProjectRunTest extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/codestarts-run-test");

    @BeforeAll
    static void setUp() throws IOException {
        ProjectTestUtil.delete(testDirPath.toFile());
    }

    private Map<String, Object> getTestInputData() {
        return getTestInputData(null);
    }

    private Map<String, Object> getTestInputData(final Map<String, Object> override) {
        return CodestartProjectGenerationTest.getTestInputData(getPlatformDescriptor(), override);
    }

    private Stream<Arguments> provideGenerateCombinations() throws IOException {
        final List<String> examples = CodestartLoader
                .loadCodestartsFromExtensions(QuarkusCodestarts.resourceLoader(getPlatformDescriptor())).stream()
                .filter(c -> c.getSpec().isExample())
                .map(Codestart::getName)
                .collect(Collectors.toList());
        return Stream.of("java", "kotlin", "scala")
                .flatMap(l -> Stream.of(Collections.emptyList(), examples).map(c -> Arguments.of(l, c)));
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
                .putData("buildtool.name", buildtool)
                // for JVM 8 and 14 this will generate project with java 1.8, for JVM 11 project with java 11
                .putData("java.version", System.getProperty("java.specification.version"))
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        Path projectDir = testDirPath.resolve(name);
        Codestarts.generateProject(codestartProject, projectDir);
        CodestartProjectTestRunner.run(projectDir, CodestartProjectTestRunner.Wrapper.fromBuildtool(buildtool));
    }
}

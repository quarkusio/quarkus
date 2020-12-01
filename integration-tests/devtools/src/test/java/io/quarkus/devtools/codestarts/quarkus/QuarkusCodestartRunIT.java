package io.quarkus.devtools.codestarts.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
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

import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;
import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartProjectDefinition;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Tag;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.DataKey;
import io.quarkus.devtools.project.BuildTool;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuarkusCodestartRunIT extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/quarkus-codestart-run-test");

    private static final Set<String> EXCLUDED = Sets.newHashSet(
            "azure-functions-http-example", "commandmode-example");

    @BeforeAll
    static void setUp() throws IOException {
        ProjectTestUtil.delete(testDirPath.toFile());
    }

    private Map<String, Object> getTestInputData() {
        return getTestInputData(null);
    }

    private Map<String, Object> getTestInputData(final Map<String, Object> override) {
        return QuarkusCodestartGenerationTest.getTestInputData(getPlatformDescriptor(), override);
    }

    private Stream<Arguments> provideGenerateCombinations() throws IOException {
        final List<Codestart> examples = getCatalog().getCodestarts().stream()
                .filter(QuarkusCodestartCatalog::isExample)
                .filter(c -> !EXCLUDED.contains(c.getName()))
                .collect(Collectors.toList());
        final List<List<String>> runAlone = examples.stream()
                .filter(c -> c.containsTag(Tag.SINGLETON_EXAMPLE.getKey()))
                .map(Codestart::getName)
                .map(Collections::singletonList)
                .collect(Collectors.toList());
        final List<String> runTogether = examples.stream()
                .filter(c -> !c.containsTag(Tag.SINGLETON_EXAMPLE.getKey()))
                .map(Codestart::getName)
                .collect(Collectors.toList());
        return Stream.of("java", "kotlin", "scala")
                .flatMap(l -> Stream.concat(Stream.of(runTogether), Stream.of(runAlone.toArray()))
                        .map(c -> Arguments.of(l, c)));
    }

    @ParameterizedTest
    @MethodSource("provideGenerateCombinations")
    public void generateMavenProjectRun(String language, List<String> codestarts) throws Exception {
        generateProjectRunTests("maven", language, codestarts, Collections.emptyMap());
    }

    @ParameterizedTest
    @MethodSource("provideGenerateCombinations")
    public void generateGradleProjectRun(String language, List<String> codestarts) throws Exception {
        generateProjectRunTests("gradle", language, codestarts, Collections.emptyMap());
    }

    @ParameterizedTest
    @MethodSource("provideGenerateCombinations")
    public void generateGradleKotlinProjectRun(String language, List<String> codestarts) throws Exception {
        generateProjectRunTests("gradle-kotlin-dsl", language, codestarts, Collections.emptyMap());
    }

    @Test
    public void generateAzureFunctionsHttpExampleProjectRun() throws Exception {
        generateProjectRunTests("maven", "java", Collections.singletonList("azure-functions-http-example"),
                Collections.emptyMap());
    }

    @Test
    public void generateCustomizedRESTEasyProjectRun() throws Exception {
        final HashMap<String, Object> data = new HashMap<>();
        data.put(DataKey.PROJECT_PACKAGE_NAME.getKey(), "com.test.andy");
        data.put(DataKey.RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "AndyEndpoint");
        data.put(DataKey.RESTEASY_EXAMPLE_RESOURCE_PATH.getKey(), "/andy");
        final String buildTool = "maven";
        final String language = "java";
        final List<String> codestarts = Collections.singletonList("resteasy-example");
        generateProjectRunTests(buildTool, language, codestarts, data,
                genName(buildTool, language, codestarts) + "-customized");
    }

    @Test
    public void generateCustomizedSpringWebProjectRun() throws Exception {
        final HashMap<String, Object> data = new HashMap<>();
        data.put(DataKey.PROJECT_PACKAGE_NAME.getKey(), "com.test.spring.web");
        data.put(DataKey.SPRING_WEB_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "SpringWebEndpoint");
        data.put(DataKey.SPRING_WEB_EXAMPLE_RESOURCE_PATH.getKey(), "/springweb");
        final String buildTool = "maven";
        final String language = "java";
        final List<String> codestarts = Collections.singletonList("spring-web-example");
        generateProjectRunTests(buildTool, language, codestarts, data,
                genName(buildTool, language, codestarts) + "-customized");
    }

    @Test
    public void generateCustomizedCommandModeProjectRun() throws Exception {
        final HashMap<String, Object> data = new HashMap<>();
        data.put(DataKey.PROJECT_PACKAGE_NAME.getKey(), "com.test.andy");
        data.put(DataKey.COMMANDMODE_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "AndyCommando");
        final String buildTool = "maven";
        final String language = "java";
        final List<String> codestarts = Collections.emptyList();
        generateProjectRunTests(buildTool, language, codestarts, data,
                genName(buildTool, language, codestarts) + "-customized");
    }

    private void generateProjectRunTests(String buildTool, String language, List<String> codestarts, Map<String, Object> data)
            throws Exception {
        generateProjectRunTests(buildTool, language, codestarts, data, genName(buildTool, language, codestarts));
    }

    private void generateProjectRunTests(String buildToolName, String language, List<String> codestarts,
            Map<String, Object> data, String name)
            throws Exception {
        final BuildTool buildTool = BuildTool.findTool(buildToolName);

        // for JVM 8 and 14 this will generate project with java 1.8, for JVM 11 project with java 11
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getTestInputData(Collections.singletonMap("artifact-id", name)))
                .buildTool(buildTool)
                .addCodestarts(codestarts)
                .addCodestart(language)
                .addData(data)
                .putData(DataKey.JAVA_VERSION.getKey(), System.getProperty("java.specification.version"))
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
        return QuarkusCodestartCatalog.fromQuarkusPlatformDescriptor(getPlatformDescriptor());
    }
}

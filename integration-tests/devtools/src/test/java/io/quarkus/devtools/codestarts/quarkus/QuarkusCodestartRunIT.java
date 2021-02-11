package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.*;
import static io.quarkus.platform.tools.ToolsUtils.readQuarkusProperties;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartProjectDefinition;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Tag;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.testing.SnapshotTesting;
import io.quarkus.devtools.testing.WrapperRunner;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuarkusCodestartRunIT extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/quarkus-codestart-run-test");

    private static final Set<String> EXCLUDED = Sets.newHashSet(
            "azure-functions-http-example", "commandmode-example");

    private static final Set<String> RUN_ALONE = Sets.newHashSet("resteasy-reactive-example", "picocli-example");

    @BeforeAll
    static void setUp() throws IOException {
        SnapshotTesting.deleteTestDirectory(testDirPath.toFile());
    }

    @Test
    public void testRunTogetherCodestartsJava() throws Exception {
        generateProjectRunTests("maven", "java", getRunTogetherExamples(), Collections.emptyMap());
    }

    @Test
    public void testRunTogetherCodestartsKotlin() throws Exception {
        generateProjectRunTests("maven", "kotlin", getRunTogetherExamples(), Collections.emptyMap());
    }

    @Test
    public void testRunTogetherCodestartsScala() throws Exception {
        generateProjectRunTests("maven", "scala", getRunTogetherExamples(), Collections.emptyMap());
    }

    @ParameterizedTest
    @MethodSource("provideLanguages")
    public void testGradle(String language) throws Exception {
        final List<String> codestarts = getRunTogetherExamples();
        generateProjectRunTests("gradle", language, codestarts, Collections.emptyMap());
    }

    @ParameterizedTest
    @MethodSource("provideLanguages")
    public void testGradleKotlinDSL(String language) throws Exception {
        final List<String> codestarts = getRunTogetherExamples();
        generateProjectRunTests("gradle-kotlin-dsl", language, codestarts, Collections.emptyMap());
    }

    @ParameterizedTest
    @MethodSource("provideRunAloneExamples")
    public void testRunAloneCodestartsJava(String codestart) throws Exception {
        generateProjectRunTests("maven", "java", singletonList(codestart), Collections.emptyMap());
    }

    @ParameterizedTest
    @MethodSource("provideRunAloneExamples")
    public void testRunAloneCodestartsKotlin(String codestart) throws Exception {
        generateProjectRunTests("maven", "kotlin", singletonList(codestart), Collections.emptyMap());
    }

    @ParameterizedTest
    @MethodSource("provideRunAloneExamples")
    public void testRunAloneCodestartsScala(String codestart) throws Exception {
        generateProjectRunTests("maven", "scala", singletonList(codestart), Collections.emptyMap());
    }

    @Test
    public void generateAzureFunctionsHttpExampleProjectRun() throws Exception {
        generateProjectRunTests("maven", "java", singletonList("azure-functions-http-example"),
                Collections.emptyMap());
    }

    @Test
    public void generateCustomizedRESTEasyProjectRun() throws Exception {
        final HashMap<String, Object> data = new HashMap<>();
        data.put(QuarkusDataKey.PROJECT_PACKAGE_NAME.key(), "com.test.andy");
        data.put(QuarkusDataKey.RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.key(), "AndyEndpoint");
        data.put(QuarkusDataKey.RESTEASY_EXAMPLE_RESOURCE_PATH.key(), "/andy");
        final String buildTool = "maven";
        final String language = "java";
        final List<String> codestarts = singletonList("resteasy-example");
        generateProjectRunTests(buildTool, language, codestarts, data,
                genName(buildTool, language, codestarts) + "-customized");
    }

    @Test
    public void generateCustomizedSpringWebProjectRun() throws Exception {
        final HashMap<String, Object> data = new HashMap<>();
        data.put(QuarkusDataKey.PROJECT_PACKAGE_NAME.key(), "com.test.spring.web");
        data.put(QuarkusDataKey.SPRING_WEB_EXAMPLE_RESOURCE_CLASS_NAME.key(), "SpringWebEndpoint");
        data.put(QuarkusDataKey.SPRING_WEB_EXAMPLE_RESOURCE_PATH.key(), "/springweb");
        final String buildTool = "maven";
        final String language = "java";
        final List<String> codestarts = singletonList("spring-web-example");
        generateProjectRunTests(buildTool, language, codestarts, data,
                genName(buildTool, language, codestarts) + "-customized");
    }

    @Test
    public void generateCustomizedCommandModeProjectRun() throws Exception {
        final HashMap<String, Object> data = new HashMap<>();
        data.put(QuarkusDataKey.PROJECT_PACKAGE_NAME.key(), "com.test.andy");
        data.put(QuarkusDataKey.COMMANDMODE_EXAMPLE_RESOURCE_CLASS_NAME.key(), "AndyCommando");
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
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        Path projectDir = testDirPath.resolve(name);
        projectDefinition.generate(projectDir);

        final int result = WrapperRunner.run(projectDir,
                WrapperRunner.Wrapper.fromBuildtool(buildToolName));
        assertThat(result).isZero();
    }

    private Map<String, Object> getTestInputData() {
        return getTestInputData(null);
    }

    private Map<String, Object> getTestInputData(final Map<String, Object> override) {
        return getTestInputData(getPlatformDescriptor(), override);
    }

    private static Map<String, Object> getTestInputData(final QuarkusPlatformDescriptor descriptor,
            final Map<String, Object> override) {
        final HashMap<String, Object> data = new HashMap<>();
        final Properties quarkusProp = readQuarkusProperties(descriptor);
        data.put(PROJECT_GROUP_ID.key(), "org.test");
        data.put(PROJECT_ARTIFACT_ID.key(), "test-codestart");
        data.put(PROJECT_VERSION.key(), "1.0.0-codestart");
        data.put(BOM_GROUP_ID.key(), descriptor.getBomGroupId());
        data.put(BOM_ARTIFACT_ID.key(), descriptor.getBomArtifactId());
        data.put(BOM_VERSION.key(), descriptor.getBomVersion());
        data.put(QUARKUS_VERSION.key(), descriptor.getQuarkusVersion());
        data.put(QUARKUS_MAVEN_PLUGIN_GROUP_ID.key(), ToolsUtils.getMavenPluginGroupId(quarkusProp));
        data.put(QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID.key(), ToolsUtils.getMavenPluginArtifactId(quarkusProp));
        data.put(QUARKUS_MAVEN_PLUGIN_VERSION.key(), ToolsUtils.getMavenPluginVersion(quarkusProp));
        data.put(QUARKUS_GRADLE_PLUGIN_ID.key(), ToolsUtils.getMavenPluginGroupId(quarkusProp));
        data.put(QUARKUS_GRADLE_PLUGIN_VERSION.key(), ToolsUtils.getGradlePluginVersion(quarkusProp));
        data.put(JAVA_VERSION.key(), System.getProperty("java.specification.version"));
        data.put(KOTLIN_VERSION.key(), quarkusProp.getProperty(ToolsConstants.PROP_KOTLIN_VERSION));
        data.put(SCALA_VERSION.key(), quarkusProp.getProperty(ToolsConstants.PROP_SCALA_VERSION));
        data.put(SCALA_MAVEN_PLUGIN_VERSION.key(), quarkusProp.getProperty(ToolsConstants.PROP_SCALA_PLUGIN_VERSION));
        data.put(MAVEN_COMPILER_PLUGIN_VERSION.key(), quarkusProp.getProperty(ToolsConstants.PROP_COMPILER_PLUGIN_VERSION));
        data.put(MAVEN_SUREFIRE_PLUGIN_VERSION.key(), quarkusProp.getProperty(ToolsConstants.PROP_SUREFIRE_PLUGIN_VERSION));
        if (override != null)
            data.putAll(override);
        return data;
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

    private List<String> getRunTogetherExamples() throws IOException {
        return getAllExamples()
                .filter(c -> !isRunAloneExample(c))
                .map(Codestart::getName)
                .collect(Collectors.toList());
    }

    private Stream<Arguments> provideRunAloneExamples() throws IOException {
        return getAllExamples()
                .filter(this::isRunAloneExample)
                .map(Codestart::getName)
                .map(Arguments::of);
    }

    private Stream<Arguments> provideLanguages() {
        return Stream.of("java", "kotlin", "scala")
                .map(Arguments::of);
    }

    private Stream<Codestart> getAllExamples() throws IOException {
        return getCatalog().getCodestarts().stream()
                .filter(QuarkusCodestartCatalog::isExample)
                .filter(c -> !EXCLUDED.contains(c.getName()));
    }

    private boolean isRunAloneExample(Codestart c) {
        return c.containsTag(Tag.SINGLETON_EXAMPLE.key()) || RUN_ALONE.contains(c.getName());
    }
}

package io.quarkus.devtools.testing.codestarts;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.PROJECT_PACKAGE_NAME;
import static io.quarkus.devtools.testing.RegistryClientTestHelper.disableRegistryClientTestConfig;
import static io.quarkus.devtools.testing.RegistryClientTestHelper.enableRegistryClientTestConfig;
import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;
import static io.quarkus.devtools.testing.SnapshotTesting.normalizePathAsName;
import static io.quarkus.devtools.testing.codestarts.QuarkusCodestartTesting.getMockedTestInputData;
import static io.quarkus.devtools.testing.codestarts.QuarkusCodestartTesting.getRealTestInputData;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartProjectInput;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.SnapshotTesting;
import io.quarkus.devtools.testing.WrapperRunner;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.AbstractPathAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class QuarkusCodestartTest implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    private static final String DEFAULT_PACKAGE_DIR = "org.acme";
    public static final String DEFAULT_PACKAGE_FOR_TESTING = "ilove.quark.us";

    private final Set<String> codestarts;
    private final Set<Language> languages;
    private final AtomicReference<ExtensionContext> currentTestContext = new AtomicReference<>();
    private final BuildTool buildTool;
    private final boolean skipGenerateRealDataProject;
    private final boolean skipGenerateMockedDataProject;
    private Path targetDir;
    private ExtensionCatalog extensionCatalog;
    private QuarkusCodestartCatalog quarkusCodestartCatalog;

    QuarkusCodestartTest(QuarkusCodestartTestBuilder builder) {
        this.codestarts = builder.codestarts;
        this.languages = builder.languages;
        this.buildTool = builder.buildTool;
        skipGenerateRealDataProject = builder.skipGenerateRealDataProject;
        skipGenerateMockedDataProject = builder.skipGenerateMockedDataProject;
    }

    public static QuarkusCodestartTestBuilder builder() {
        return new QuarkusCodestartTestBuilder();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        enableRegistryClientTestConfig();
        targetDir = Paths.get("target/quarkus-codestart-test/" + getTestId());
        SnapshotTesting.deleteTestDirectory(targetDir.toFile());
        for (Language language : languages) {
            if (!skipGenerateMockedDataProject) {
                generateMockedDataProject(language);
            }
            if (!skipGenerateRealDataProject) {
                generateRealDataProject(language);
            }
        }
    }

    private String getTestId() {
        String tool = buildTool != null ? buildTool.getKey() + "-" : "";
        return tool + String.join("-", codestarts);
    }

    public QuarkusCodestartCatalog getQuarkusCodestartCatalog() throws IOException {
        return quarkusCodestartCatalog == null
                ? quarkusCodestartCatalog = QuarkusCodestartCatalog.fromExtensionsCatalog(getExtensionsCatalog(),
                        getCodestartsResourceLoaders())
                : quarkusCodestartCatalog;
    }

    public ExtensionCatalog getExtensionsCatalog() {
        if (extensionCatalog == null) {
            try {
                extensionCatalog = QuarkusProjectHelper.getCatalogResolver().resolveExtensionCatalog();
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve extensions catalog", e);
            }
        }
        return extensionCatalog;
    }

    private void generateRealDataProject(Language language) throws IOException {
        generateProject(getProjectWithRealDataDir(language), language,
                getRealTestInputData(getExtensionsCatalog(), Collections.emptyMap()));
    }

    private void generateMockedDataProject(Language language) throws IOException {
        generateProject(getProjectWithMockedDataDir(language), language, getMockedTestInputData(Collections.emptyMap()));
    }

    public void buildAllProjects() {
        for (Language language : languages) {
            buildProject(language);
        }
    }

    public void buildProject(Language language) {
        final int exitCode = WrapperRunner.run(getProjectWithRealDataDir(language));
        Assertions.assertThat(exitCode).as("Run project return status is zero").isZero();
    }

    public void checkGeneratedSource(String className) throws Throwable {
        for (Language language : languages) {
            checkGeneratedSource(language, className);
        }
    }

    public AbstractPathAssert<?> assertThatGeneratedFile(Language language, String fileRelativePat) throws Throwable {
        return Assertions.assertThat(this.getProjectWithMockedDataDir(language).resolve(fileRelativePat));
    }

    public void checkGeneratedTestSource(String className) throws Throwable {
        for (Language language : languages) {
            checkGeneratedTestSource(language, className);
        }
    }

    public AbstractPathAssert<?> checkGeneratedSource(Language language, String className) throws Throwable {
        return checkGeneratedSource("main", language, className);
    }

    public AbstractPathAssert<?> checkGeneratedTestSource(Language language, String className) throws Throwable {
        return checkGeneratedSource("test", language, className);
    }

    public AbstractPathAssert<?> checkGeneratedSource(String sourceDir, Language language, String className) throws Throwable {
        final String modifiedClassName = className.replace(DEFAULT_PACKAGE_DIR, DEFAULT_PACKAGE_FOR_TESTING).replace(".", "/");
        final String fileRelativePath = "src/" + sourceDir + "/" + language.key() + "/" + modifiedClassName
                + getSourceFileExtension(language);
        return assertThatGeneratedFileMatchSnapshot(language, fileRelativePath)
                .satisfies(checkContains("package " + DEFAULT_PACKAGE_FOR_TESTING));
    }

    public AbstractPathAssert<?> assertThatGeneratedFileMatchSnapshot(Language language, String fileRelativePath)
            throws Throwable {
        ExtensionContext context = Objects.requireNonNull(currentTestContext.get(), "Context must be present");
        final String snapshotDirName = context.getTestClass().get().getSimpleName() + "/"
                + context.getTestMethod().get().getName();
        final String normalizedFileName = snapshotDirName + "/" + normalizePathAsName(fileRelativePath);
        return SnapshotTesting.assertThatMatchSnapshot(getProjectWithMockedDataDir(language).resolve(fileRelativePath),
                normalizedFileName);
    }

    public ListAssert<String> assertThatGeneratedTreeMatchSnapshots(Language language) throws Throwable {
        return assertThatGeneratedTreeMatchSnapshots(language, null);
    }

    public ListAssert<String> assertThatGeneratedTreeMatchSnapshots(Language language, String dirRelativePath)
            throws Throwable {
        ExtensionContext context = Objects.requireNonNull(currentTestContext.get(), "Context must be present");
        final String snapshotDirName = context.getTestClass().get().getSimpleName() + "/"
                + context.getTestMethod().get().getName();
        final Path dir = dirRelativePath != null ? getProjectWithMockedDataDir(language).resolve(dirRelativePath)
                : getProjectWithMockedDataDir(language);
        return SnapshotTesting.assertThatDirectoryTreeMatchSnapshots(snapshotDirName, dir);
    }

    private void generateProject(Path projectDir, Language language, Map<String, Object> inputData) throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addCodestart(language.key())
                .buildTool(buildTool)
                .addCodestarts(codestarts)
                .addData(inputData)
                .putData(PROJECT_PACKAGE_NAME, DEFAULT_PACKAGE_FOR_TESTING)
                .build();
        getQuarkusCodestartCatalog().createProject(input).generate(projectDir);
    }

    private Path getProjectWithRealDataDir(Language language) {
        return targetDir.resolve("real-data").resolve(language.key());
    }

    private Path getProjectWithMockedDataDir(Language language) {
        return targetDir.resolve("mocked-data").resolve(language.key());
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        disableRegistryClientTestConfig();
    }

    protected List<ResourceLoader> getCodestartsResourceLoaders() {
        return QuarkusProjectHelper.getCodestartResourceLoaders(getExtensionsCatalog());
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        currentTestContext.set(null);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        currentTestContext.set(extensionContext);
    }

    private String getSourceFileExtension(Language language) {
        return Language.KOTLIN.equals(language) ? ".kt" : "." + language.key();
    }
}

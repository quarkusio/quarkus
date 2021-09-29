package io.quarkus.devtools.testing.codestarts;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.PROJECT_PACKAGE_NAME;
import static io.quarkus.devtools.project.CodestartResourceLoadersBuilder.codestartLoadersBuilder;
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
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

/**
 * This extension helps test a Quarkus extension codestart. It provides a way to test:
 * <ul>
 * <li>the generated project content (with immutable mocked data) using snapshot testing</li>
 * <li>the generated project build/run (with real data) with helpers to run the build</li>
 * </ul>
 * <br>
 * Before all tests, the extension will generate Quarkus projects in the specified languages with the given codestart,
 * with mocked data and with real data.
 * <br>
 * You can find those generated project in `target/quarkus-codestart-test`.
 * <br>
 * You can open the `real-data` ones in your IDE or play with them using the terminal.
 * <br>
 * <b></b>Running those tests is the easiest way to iterate on your extension codestart dev</b>
 */
public class QuarkusCodestartTest implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    private static final String DEFAULT_PACKAGE_DIR = "org.acme";

    private final Set<String> codestarts;
    private final Set<Language> languages;
    private final Map<String, Object> data;
    private final AtomicReference<ExtensionContext> currentTestContext = new AtomicReference<>();
    private final BuildTool buildTool;
    private final Set<Language> hasGeneratedProjectsWithMockedData = new HashSet<>();
    private final Set<Language> hasGeneratedProjectsWithRealData = new HashSet<>();
    private final boolean enableRegistryClient;
    private final Collection<String> artifacts;
    private final Collection<ArtifactCoords> extensions;
    private Path targetDir;
    private ExtensionCatalog extensionCatalog;
    private QuarkusCodestartCatalog quarkusCodestartCatalog;
    private String packageName;

    QuarkusCodestartTest(QuarkusCodestartTestBuilder builder) {
        this.codestarts = builder.codestarts;
        this.languages = builder.languages;
        this.buildTool = builder.buildTool;
        this.quarkusCodestartCatalog = builder.quarkusCodestartCatalog;
        this.extensionCatalog = builder.extensionCatalog;
        this.enableRegistryClient = builder.extensionCatalog == null;
        this.data = builder.data;
        this.artifacts = builder.artifacts;
        this.extensions = builder.extensions;
        this.packageName = builder.packageName;
    }

    public static QuarkusCodestartTestBuilder builder() {
        return new QuarkusCodestartTestBuilder();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (enableRegistryClient) {
            enableRegistryClientTestConfig();
        }
        targetDir = Paths.get("target/quarkus-codestart-test/" + getTestId());
        SnapshotTesting.deleteTestDirectory(targetDir.toFile());
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
                throw new RuntimeException("Failed to resolve extension catalog", e);
            }
        }
        return extensionCatalog;
    }

    /**
     * This will run the build on all generated projects (with real data)
     * 
     * @throws IOException
     */
    public void buildAllProjects() throws IOException {
        for (Language language : languages) {
            buildProject(language);
        }
    }

    /**
     * This will run the build on the generated projects (with real data) in the specified language.
     *
     *
     * @param language the language
     * @throws IOException
     */
    public void buildProject(Language language) throws IOException {
        final int exitCode = WrapperRunner.run(getProjectWithRealDataDir(language));
        Assertions.assertThat(exitCode).as("Run project return status is zero").isZero();
    }

    /**
     * Check that the given full qualified className is valid in all generated projects (with fake data)
     * <br>
     * <br>
     * See {@link QuarkusCodestartTest#checkGeneratedSource(Language language, String className)}
     *
     * @param className the full qualified className (using `org.acme.ClassName` also works, it will be replaced by the project
     *        package name)
     * @throws Throwable
     */
    public void checkGeneratedSource(String className) throws Throwable {
        for (Language language : languages) {
            checkGeneratedSource(language, className);
        }
    }

    /**
     * Check that the given full qualified test className is valid in all generated projects (with fake data)
     * <br>
     * <br>
     * See {@link QuarkusCodestartTest#checkGeneratedSource(Language language, String className)}
     *
     * @param className the full qualified test class name (using `org.acme.ClassName` also works, it will be replaced by the
     *        project package name)
     * @throws Throwable
     */
    public void checkGeneratedTestSource(String className) throws Throwable {
        for (Language language : languages) {
            checkGeneratedTestSource(language, className);
        }
    }

    /**
     * It will validate (compare and check package name) the class against the snapshots in all the projects for the given
     * language
     * 
     * @param language the language to check
     * @param className the full qualified className (using `org.acme.ClassName` also works, it will be replaced by the project
     *        package name)
     * @return
     * @throws Throwable
     */
    public AbstractPathAssert<?> checkGeneratedSource(Language language, String className) throws Throwable {
        return checkGeneratedSource("main", language, className);
    }

    /**
     * It will validate (compare and check package name) the test class against the snapshots in all the projects for the given
     * language
     * 
     * @param language the language to check
     * @param className the full qualified test className (using `org.acme.ClassName` also works, it will be replaced by the
     *        project package name)
     * @return
     * @throws Throwable
     */
    public AbstractPathAssert<?> checkGeneratedTestSource(Language language, String className) throws Throwable {
        return checkGeneratedSource("test", language, className);
    }

    /**
     * Get a PathAssert on a generated mocked file for a specific language
     *
     * <br>
     * <br>
     *
     * Very usefull to check if a file contains a specific String:
     * <br>
     * Example:<br>
     * codestartTest.assertThatGeneratedFile(JAVA, "README.md").satisfies(checkContains("./mvnw compile quarkus:dev
     * -Dquarkus.args='Quarky"));
     *
     * @param language the language
     * @param fileRelativePath the relative path for the file in the generated project
     * @return the PathAssert
     * @throws Throwable
     */
    public AbstractPathAssert<?> assertThatGeneratedFile(Language language, String fileRelativePath) throws Throwable {
        return Assertions.assertThat(this.getProjectWithMockedDataDir(language).resolve(fileRelativePath));
    }

    /**
     * See {@link #assertThatGeneratedFile(Language language, String fileRelativePath)}
     * but also compare it with its snapshots
     *
     * @param language the language
     * @param fileRelativePath the relative path for the file in the generated project
     * @return
     * @throws Throwable
     */
    public AbstractPathAssert<?> assertThatGeneratedFileMatchSnapshot(Language language, String fileRelativePath)
            throws Throwable {
        ExtensionContext context = Objects.requireNonNull(currentTestContext.get(), "Context must be present");
        final String snapshotDirName = context.getTestClass().get().getSimpleName() + "/"
                + context.getTestMethod().get().getName();
        final String normalizedFileName = snapshotDirName + "/" + normalizePathAsName(fileRelativePath);
        return SnapshotTesting.assertThatMatchSnapshot(getProjectWithMockedDataDir(language).resolve(fileRelativePath),
                normalizedFileName);
    }

    /**
     * This let you compare the project file structure (tree) for a specific language against its snapshot
     * 
     * @param language the language
     * @return the ListAssert
     * @throws Throwable
     */
    public ListAssert<String> assertThatGeneratedTreeMatchSnapshots(Language language) throws Throwable {
        return assertThatGeneratedTreeMatchSnapshots(language, null);
    }

    /**
     * see {link #assertThatGeneratedTreeMatchSnapshots(Language language)}
     * but for a specific sub directory
     * 
     * @param language the language
     * @param dirRelativePath the sub directory
     * @return the ListAssert
     * @throws Throwable
     */
    public ListAssert<String> assertThatGeneratedTreeMatchSnapshots(Language language, String dirRelativePath)
            throws Throwable {
        ExtensionContext context = Objects.requireNonNull(currentTestContext.get(), "Context must be present");
        final String snapshotDirName = context.getTestClass().get().getSimpleName() + "/"
                + context.getTestMethod().get().getName();
        final Path dir = dirRelativePath != null ? getProjectWithMockedDataDir(language).resolve(dirRelativePath)
                : getProjectWithMockedDataDir(language);
        return SnapshotTesting.assertThatDirectoryTreeMatchSnapshots(snapshotDirName, dir);
    }

    private AbstractPathAssert<?> checkGeneratedSource(String sourceDir, Language language, String className) throws Throwable {
        final String modifiedClassName = className.replace(DEFAULT_PACKAGE_DIR, packageName).replace('.', '/');
        final String fileRelativePath = "src/" + sourceDir + "/" + language.key() + "/" + modifiedClassName
                + getSourceFileExtension(language);
        return assertThatGeneratedFileMatchSnapshot(language, fileRelativePath)
                .satisfies(checkContains("package " + packageName));
    }

    private String getTestId() {
        String tool = buildTool != null ? buildTool.getKey() + "-" : "";
        return tool + String.join("-", codestarts);
    }

    private void generateRealDataProjectIfNeeded(Path path, Language language) throws IOException {
        if (!hasGeneratedProjectsWithRealData.contains(language)) {
            generateProject(path, language,
                    getRealTestInputData(getExtensionsCatalog(), Collections.emptyMap()));
        }
        hasGeneratedProjectsWithRealData.add(language);
    }

    private void generateMockedDataProjectIfNeeded(Path path, Language language) throws IOException {
        if (!hasGeneratedProjectsWithMockedData.contains(language)) {
            generateProject(path, language, getMockedTestInputData(Collections.emptyMap()));
        }
        hasGeneratedProjectsWithMockedData.add(language);
    }

    private void generateProject(Path projectDir, Language language, Map<String, Object> inputData) throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addCodestart(language.key())
                .buildTool(buildTool)
                .addCodestarts(codestarts)
                .addData(inputData)
                .addData(data)
                .addBoms(QuarkusCodestartTesting.getBoms(inputData))
                .addExtensions(extensions)
                .putData(PROJECT_PACKAGE_NAME, packageName)
                .build();
        getQuarkusCodestartCatalog().createProject(input).generate(projectDir);
    }

    private Path getProjectWithRealDataDir(Language language) throws IOException {
        final Path dir = targetDir.resolve("real-data").resolve(language.key());
        generateRealDataProjectIfNeeded(dir, language);
        return dir;
    }

    private Path getProjectWithMockedDataDir(Language language) throws IOException {
        final Path dir = targetDir.resolve("mocked-data").resolve(language.key());
        generateMockedDataProjectIfNeeded(dir, language);
        return dir;
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (enableRegistryClient) {
            disableRegistryClientTestConfig();
        }
    }

    protected List<ResourceLoader> getCodestartsResourceLoaders() {
        return codestartLoadersBuilder()
                .catalog(getExtensionsCatalog())
                .addExtraCodestartsArtifactCoords(artifacts)
                .build();
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

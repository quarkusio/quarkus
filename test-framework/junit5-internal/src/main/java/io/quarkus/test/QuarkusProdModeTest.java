package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.jboss.logmanager.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.platform.commons.JUnitException;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.builder.BuildStep;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.utilities.JavaBinFinder;

/**
 * A test extension for producing a prod-mode jar. This is meant to be used by extension authors, it's not intended for end user
 * consumption
 */
public class QuarkusProdModeTest
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, TestWatcher, InvocationInterceptor {

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final int DEFAULT_HTTP_PORT_INT = 8081;
    private static final String DEFAULT_HTTP_PORT = "" + DEFAULT_HTTP_PORT_INT;
    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";

    static final String BUILD_CONTEXT_BUILD_STEP_ENTRIES = "buildStepEntries";
    static final String BUILD_CONTEXT_BUILD_STEP_ENTRY_CONSUMES = "buildStepEntryConsumes";
    static final String BUILD_CONTEXT_BUILD_STEP_ENTRY_PRODUCES = "buildStepEntryProduces";

    public static String BUILD_CONTEXT_CUSTOM_SOURCES_PATH_KEY = "customSourcesDir";

    private static final Logger rootLogger;
    private Handler[] originalHandlers;

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        rootLogger = (Logger) LogManager.getLogManager().getLogger("");
    }

    private Path outputDir;
    private Path buildDir;
    private Supplier<JavaArchive> archiveProducer;
    private String applicationName;
    private String applicationVersion;
    private boolean buildNative;

    private static final Timer timeoutTimer = new Timer("Test thread dump timer");
    private volatile TimerTask timeoutTask;
    private Properties customApplicationProperties;
    private CuratedApplication curatedApplication;

    private boolean run;
    private boolean preventOutputDirCleanup;

    private String logFileName;
    private Map<String, String> runtimeProperties;
    // by default, we use these lower heap settings
    private List<String> jvmArgs = Collections.singletonList("-Xmx128m");
    private Map<String, String> testResourceProperties = new HashMap<>();
    // these will be used to create a directory that can then be obtained by the buildChainCustomizersProducer function
    // values are meant to be resources that exist on the test classpath
    // which should be copied to the files represented by the keys
    private Map<Path, String> customSourcesMap = new HashMap<>();
    private List<BuildChainCustomizerEntry> buildChainCustomizerEntries = new ArrayList<>();

    private Process process;

    private Path builtResultArtifact;
    private ProdModeTestResults prodModeTestResults;
    private Optional<Field> prodModeTestResultsField = Optional.empty();
    private Path logfilePath;
    private Optional<Field> logfileField = Optional.empty();
    private List<Dependency> forcedDependencies = Collections.emptyList();
    private InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler((r) -> false);
    private boolean expectExit;
    private String startupConsoleOutput;
    private Integer exitCode;
    private Consumer<Throwable> assertBuildException;
    private String[] commandLineParameters = new String[0];

    public QuarkusProdModeTest() {
        InputStream appPropsIs = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties");
        if (appPropsIs != null) {
            customApplicationProperties = new Properties();
            try (InputStream is = appPropsIs) {
                customApplicationProperties.load(is);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load application configuration from "
                        + Thread.currentThread().getContextClassLoader().getResource("application.properties"), e);
            }
        }
    }

    public Supplier<JavaArchive> getArchiveProducer() {
        return archiveProducer;
    }

    public QuarkusProdModeTest setArchiveProducer(Supplier<JavaArchive> archiveProducer) {
        Objects.requireNonNull(archiveProducer);
        this.archiveProducer = archiveProducer;
        return this;
    }

    public QuarkusProdModeTest addBuildChainCustomizerEntries(BuildChainCustomizerEntry entry) {
        Objects.requireNonNull(entry);
        this.buildChainCustomizerEntries.add(entry);
        return this;
    }

    public QuarkusProdModeTest addCustomResourceEntry(Path outputPath, String classPathLocation) {
        Objects.requireNonNull(outputPath);
        Objects.requireNonNull(classPathLocation);
        this.customSourcesMap.put(outputPath, classPathLocation);
        return this;
    }

    /**
     * Effectively sets the quarkus.application.name property.
     * This value will override quarkus.application.name if that has been set in the configuration properties
     */
    public QuarkusProdModeTest setApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    /**
     * Effectively sets the quarkus.application.version property.
     * This value will override quarkus.application.version if that has been set in the configuration properties
     */
    public QuarkusProdModeTest setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
        return this;
    }

    /**
     * Effectively sets the quarkus.packaging.type property.
     * This value will override quarkus.packaging.type if that has been set in the configuration properties
     */
    public QuarkusProdModeTest setBuildNative(boolean buildNative) {
        this.buildNative = buildNative;
        return this;
    }

    /**
     * If set to true, the built artifact will be run before starting the tests
     */
    public QuarkusProdModeTest setRun(boolean run) {
        this.run = run;
        return this;
    }

    /**
     * File where the running application logs its output
     * This property effectively sets the quarkus.log.file.path runtime configuration property
     * and will override that value if it has been set in the configuration properties of the test
     */
    public QuarkusProdModeTest setLogFileName(String logFileName) {
        this.logFileName = logFileName;
        return this;
    }

    /**
     * The complete set of JVM args to be used if the built artifact is configured to be run
     */
    public QuarkusProdModeTest setJVMArgs(final List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    /**
     * The runtime configuration properties to be used if the built artifact is configured to be run
     */
    public QuarkusProdModeTest setRuntimeProperties(Map<String, String> runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
        return this;
    }

    public QuarkusProdModeTest setLogRecordPredicate(Predicate<LogRecord> predicate) {
        this.inMemoryLogHandler = new InMemoryLogHandler(predicate);
        return this;
    }

    /**
     * Provides a convenient way to either add additional dependencies to the application (if it doesn't already contain a
     * dependency), or override a version (if the dependency already exists)
     */
    public QuarkusProdModeTest setForcedDependencies(List<Dependency> forcedDependencies) {
        this.forcedDependencies = forcedDependencies;

        return this;
    }

    /**
     * If this is true then the Quarkus application is expected to exit immediately (i.e. is a command mode app)
     */
    public QuarkusProdModeTest setExpectExit(boolean expectExit) {
        this.expectExit = expectExit;
        return this;
    }

    public QuarkusProdModeTest assertBuildException(Consumer<Throwable> assertException) {
        if (this.assertBuildException != null) {
            throw new IllegalStateException("Don't set the asserted or excepted exception twice"
                    + " to avoid shadowing out the first call.");
        }
        this.assertBuildException = assertException;
        return this;
    }

    public QuarkusProdModeTest setExpectedException(Class<? extends Throwable> expectedException) {
        return assertBuildException(t -> {
            Throwable i = t;
            boolean found = false;
            while (i != null) {
                if (i.getClass().getName().equals(expectedException.getName())) {
                    found = true;
                    break;
                }
                i = i.getCause();
            }

            assertTrue(found, "Build failed with wrong exception, expected " + expectedException + " but got " + t);
        });
    }

    /**
     * Returns the console output from startup. If {@link #expectExit} is true then this will contain
     * all the console output.
     */
    public String getStartupConsoleOutput() {
        return startupConsoleOutput;
    }

    /**
     * Returns the process exit code, this can only be used if {@link #expectExit} is true.
     * Null if the app is running.
     */
    public Integer getExitCode() {
        return exitCode;
    }

    private void exportArchive(Path deploymentDir, Class<?> testClass) {
        try {
            JavaArchive archive = getArchiveProducerOrDefault();
            if (customApplicationProperties != null) {
                archive.add(new PropertiesAsset(customApplicationProperties), "application.properties");
            }
            archive.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.toFile());

            String exportPath = System.getProperty("quarkus.deploymentExportPath");
            if (exportPath != null) {
                File exportDir = new File(exportPath);
                if (exportDir.exists()) {
                    if (!exportDir.isDirectory()) {
                        throw new IllegalStateException("Export path is not a directory: " + exportPath);
                    }
                    try (Stream<Path> stream = Files.walk(exportDir.toPath())) {
                        stream.sorted(Comparator.reverseOrder()).map(Path::toFile)
                                .forEach(File::delete);
                    }
                } else if (!exportDir.mkdirs()) {
                    throw new IllegalStateException("Export path could not be created: " + exportPath);
                }
                File exportFile = new File(exportDir, archive.getName());
                archive.as(ZipExporter.class).exportTo(exportFile);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to create the archive", e);
        }
    }

    private JavaArchive getArchiveProducerOrDefault() {
        if (archiveProducer == null) {
            return ShrinkWrap.create(JavaArchive.class);
        } else {
            return archiveProducer.get();
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        ensureNoInjectAnnotationIsUsed(extensionContext.getRequiredTestClass());
        ExclusivityChecker.checkTestType(extensionContext, QuarkusProdModeTest.class);

        originalHandlers = rootLogger.getHandlers();
        rootLogger.addHandler(inMemoryLogHandler);

        timeoutTask = new PrintStackTraceTimerTask();
        timeoutTimer.schedule(timeoutTask, 1000 * 60 * 5);

        ExtensionContext.Store store = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        if (store.get(TestResourceManager.class.getName()) == null) {
            TestResourceManager manager = new TestResourceManager(extensionContext.getRequiredTestClass());
            manager.init();
            testResourceProperties = manager.start();
            store.put(TestResourceManager.class.getName(), manager);
            store.put(TestResourceManager.CLOSEABLE_NAME, new ExtensionContext.Store.CloseableResource() {

                @Override
                public void close() throws Throwable {
                    manager.close();
                }
            });
        }

        Class<?> testClass = extensionContext.getRequiredTestClass();

        try {
            outputDir = Files.createTempDirectory("quarkus-prod-mode-test");
            Path deploymentDir = outputDir.resolve("deployment-result");
            buildDir = outputDir.resolve("build-result");

            if (applicationName != null) {
                overrideConfigKey("quarkus.application.name", applicationName);
            }
            if (applicationVersion != null) {
                overrideConfigKey("quarkus.application.version", applicationVersion);
            }
            if (buildNative) {
                overrideConfigKey("quarkus.package.type", "native");
            }
            exportArchive(deploymentDir, testClass);

            Path testLocation = PathTestHelper.getTestClassesLocation(testClass);

            Path customSourcesDir = createCustomSources(testClass);

            // This is a bit of a hack but if the current project does not contain any
            // sources nor resources, we need to create an empty classes dir to satisfy the resolver
            // as this project will appear as the root application artifact during the bootstrap
            if (Files.isDirectory(testLocation)) {
                final Path projectClassesDir = PathTestHelper.getAppClassLocationForTestLocation(testLocation.toString());
                if (!Files.exists(projectClassesDir)) {
                    Files.createDirectories(projectClassesDir);
                }
            }
            QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                    .setApplicationRoot(deploymentDir)
                    .setMode(QuarkusBootstrap.Mode.PROD)
                    .setLocalProjectDiscovery(true)
                    .setIsolateDeployment(true)
                    .addExcludedPath(testLocation)
                    .setProjectRoot(testLocation)
                    .setTargetDirectory(buildDir)
                    .setForcedDependencies(forcedDependencies);
            if (applicationName != null) {
                builder.setBaseName(applicationName);
            }

            Map<String, Object> buildContext = new HashMap<>();
            buildContext.put(BUILD_CONTEXT_CUSTOM_SOURCES_PATH_KEY, customSourcesDir);

            if (!buildChainCustomizerEntries.isEmpty()) {
                // we need to make sure all the classes needed to support the customizer flow are available at bootstrap time
                // for that purpose we add them to a new archive that is then added to Quarkus bootstrap
                Path additionalDeploymentDir = Files.createDirectories(outputDir.resolve("additional-deployment"));
                JavaArchive additionalDeploymentArchive = ShrinkWrap.create(JavaArchive.class)
                        .addClasses(ProdModeTestBuildChainCustomizerProducer.class, ProdModeTestBuildChainBuilderConsumer.class,
                                ProdModeTestBuildStep.class);

                // we push data from the test extension down to the customizers via JDK classes only because this data needs to be
                // accessible by different classloaders
                Map<Object, Object> entriesMap = new HashMap<>();
                buildContext.put(BUILD_CONTEXT_BUILD_STEP_ENTRIES, entriesMap);

                for (BuildChainCustomizerEntry entry : buildChainCustomizerEntries) {
                    additionalDeploymentArchive.addClasses(entry.getBuildStepClass());
                    entriesMap.put(entry.getBuildStepClass().getName(),
                            Map.of(BUILD_CONTEXT_BUILD_STEP_ENTRY_PRODUCES,
                                    entry.getProduces().stream().map(Class::getName).collect(Collectors.toList()),
                                    BUILD_CONTEXT_BUILD_STEP_ENTRY_CONSUMES,
                                    entry.getConsumes().stream().map(Class::getName).collect(Collectors.toList())));
                }
                additionalDeploymentArchive.as(ExplodedExporter.class)
                        .exportExplodedInto(additionalDeploymentDir.toFile());
                builder.addAdditionalDeploymentArchive(additionalDeploymentDir);
            }
            curatedApplication = builder.build().bootstrap();

            AugmentAction action;
            if (buildChainCustomizerEntries.isEmpty()) {
                action = curatedApplication.createAugmentor();
            } else {
                action = curatedApplication.createAugmentor(ProdModeTestBuildChainCustomizerProducer.class.getName(),
                        buildContext);
            }
            AugmentResult result;
            try {
                result = action.createProductionApplication();
                if (assertBuildException != null) {
                    fail("The build was expected to fail");
                }
            } catch (Exception e) {
                if (assertBuildException != null) {
                    assertBuildException.accept(e);
                    return;
                } else {
                    throw e;
                }
            } finally {
                curatedApplication.close();
            }

            builtResultArtifact = setupProdModeResults(testClass, buildDir, result);

            if (run) {
                start();

                if (logfilePath != null) {
                    logfileField = Arrays.stream(testClass.getDeclaredFields()).filter(
                            f -> f.isAnnotationPresent(LogFile.class) && Path.class.equals(f.getType()))
                            .findAny();
                    logfileField.ifPresent(f -> f.setAccessible(true));
                }
            }

        } catch (Exception e) {
            preventOutputDirCleanup = true;
            logOutputPathForPostMortem();
            throw new RuntimeException(e);
        }
    }

    private void ensureNoInjectAnnotationIsUsed(Class<?> testClass) {
        Class<?> current = testClass;
        while (current.getSuperclass() != null) {
            for (Field field : current.getDeclaredFields()) {
                Inject injectAnnotation = field.getAnnotation(Inject.class);
                if (injectAnnotation != null) {
                    throw new JUnitException(
                            "@Inject is not supported in QuarkusProdModeTest tests. Offending field is "
                                    + field.getDeclaringClass().getTypeName() + "."
                                    + field.getName());
                }
            }
            current = current.getSuperclass();
        }

    }

    private Path createCustomSources(Class<?> testClass) throws IOException {
        Path customSourcesDir = outputDir.resolve("custom-sources");
        for (Map.Entry<Path, String> entry : customSourcesMap.entrySet()) {
            Path finalResourcePath = customSourcesDir.resolve(entry.getKey());
            Files.createDirectories(finalResourcePath.getParent());

            InputStream classPathResource = testClass.getClassLoader().getResourceAsStream(entry.getValue());
            if (classPathResource == null) {
                throw new IllegalArgumentException("Resource '" + finalResourcePath.getFileName()
                        + "' supplied as a value of customSourcesMap does not exist on the test classpath");
            }
            Files.write(finalResourcePath, classPathResource.readAllBytes());
        }
        return customSourcesDir;
    }

    private void logOutputPathForPostMortem() {
        if (buildDir != null) {
            String message = "The output of the Quarkus build can be found at " + buildDir.toAbsolutePath().toString();
            System.err.println(message);
        }
    }

    private Path setupProdModeResults(Class<?> testClass, Path buildDir, AugmentResult result) {
        prodModeTestResultsField = Arrays.stream(testClass.getDeclaredFields()).filter(
                f -> f.isAnnotationPresent(ProdBuildResults.class) && ProdModeTestResults.class.equals(f.getType()))
                .findAny();
        prodModeTestResultsField.ifPresent(f -> f.setAccessible(true));

        Path builtResultArtifact = result.getNativeResult();
        if (builtResultArtifact == null) {
            builtResultArtifact = result.getJar().getPath();
        }

        prodModeTestResults = new ProdModeTestResults(buildDir, builtResultArtifact, result.getResults(),
                inMemoryLogHandler.records);
        return builtResultArtifact;
    }

    /**
     * Start the Quarkus application. If the application is already started, it raises an {@link IllegalStateException}
     * exception.
     *
     * @throws RuntimeException when application errors at startup.
     * @throws IllegalStateException if the application is already started.
     */
    public void start() {
        if (process != null && process.isAlive()) {
            throw new IllegalStateException("Quarkus application is already started. ");
        }

        exitCode = null;
        Path builtResultArtifactParent = builtResultArtifact.getParent();

        if (runtimeProperties == null) {
            runtimeProperties = new HashMap<>();
        } else {
            // copy the use supplied properties since it might be an immutable map
            runtimeProperties = new HashMap<>(runtimeProperties);
        }
        runtimeProperties.putIfAbsent(QUARKUS_HTTP_PORT_PROPERTY, DEFAULT_HTTP_PORT);
        if (logFileName != null) {
            logfilePath = builtResultArtifactParent.resolve(logFileName);
            runtimeProperties.put("quarkus.log.file.path", logfilePath.toAbsolutePath().toString());
            runtimeProperties.put("quarkus.log.file.enable", "true");
        }

        // ensure that the properties obtained from QuarkusTestResourceLifecycleManager
        // are propagated to runtime
        runtimeProperties.putAll(testResourceProperties);

        List<String> systemProperties = runtimeProperties.entrySet().stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue()).collect(Collectors.toList());
        List<String> command = new ArrayList<>(systemProperties.size() + 3);
        if (builtResultArtifact.getFileName().toString().endsWith(".jar")) {
            command.add(JavaBinFinder.findBin());
            if (this.jvmArgs != null) {
                command.addAll(this.jvmArgs);
            }
            command.addAll(systemProperties);
            command.add("-jar");
            command.add(builtResultArtifact.toAbsolutePath().toString());
        } else {
            command.add(builtResultArtifact.toAbsolutePath().toString());
            if (this.jvmArgs != null) {
                command.addAll(this.jvmArgs);
            }
            command.addAll(systemProperties);
        }

        command.addAll(Arrays.asList(commandLineParameters));

        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .directory(builtResultArtifactParent.toFile())
                    .start();
            ensureApplicationStartupOrFailure();
            setupRestAssured();
        } catch (IOException ex) {
            throw new RuntimeException("The produced jar could not be launched. ", ex);
        }
    }

    /**
     * Stop the Quarkus application.
     */
    public void stop() {
        try {
            if (process != null) {
                process.destroy();
                process.waitFor();
                exitCode = process.exitValue();
            }
        } catch (InterruptedException ignored) {

        }
    }

    private void setupRestAssured() {
        Integer httpPort = Optional.ofNullable(runtimeProperties.get(QUARKUS_HTTP_PORT_PROPERTY))
                .map(Integer::parseInt)
                .orElse(DEFAULT_HTTP_PORT_INT);

        // If http port is 0, then we need to set the port to null in order to use the `quarkus.https.test-port` property
        // which is done in `RestAssuredURLManager.setURL`.
        if (httpPort == 0) {
            httpPort = null;
        }

        RestAssuredURLManager.setURL(false, httpPort);
    }

    private void ensureApplicationStartupOrFailure() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = in.readLine();
            if (line != null) {
                System.out.println(line);
                sb.append(line);
                sb.append("\n");
                if (!expectExit && line.contains(EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED)) {
                    in.close();
                    this.startupConsoleOutput = sb.toString();
                    break;
                }
            } else {
                //process has exited
                this.startupConsoleOutput = sb.toString();
                in.close();
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                exitCode = process.exitValue();
                if (expectExit) {
                    return;
                }
                throw new RuntimeException(
                        "The produced jar could not be launched. Consult the above output for the exact cause.");
            }
        }
    }

    @Override
    public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        doIntercept(invocation);
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        doIntercept(invocation);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        doIntercept(invocation);
    }

    private void doIntercept(Invocation<Void> invocation) throws Throwable {
        if (assertBuildException != null) {
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        preventOutputDirCleanup = true;
        logOutputPathForPostMortem();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        rootLogger.setHandlers(originalHandlers);
        inMemoryLogHandler.clearRecords();

        if (run) {
            RestAssuredURLManager.clearURL();
        }

        stop();

        try {
            if (curatedApplication != null) {
                curatedApplication.close();
                curatedApplication = null;
            }
        } finally {
            timeoutTask.cancel();
            timeoutTask = null;

            if (!buildChainCustomizerEntries.isEmpty()) {
                buildChainCustomizerEntries.clear();
                buildChainCustomizerEntries = null;
            }

            if ((outputDir != null) && !preventOutputDirCleanup) {
                FileUtil.deleteDirectory(outputDir);
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (run && (process == null || !process.isAlive())) {
            start();
        }

        prodModeTestResultsField.ifPresent(f -> {
            try {
                f.set(context.getRequiredTestInstance(), prodModeTestResults);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

        logfileField.ifPresent(f -> {
            try {
                f.set(context.getRequiredTestInstance(), logfilePath);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public QuarkusProdModeTest withConfigurationResource(String resourceName) {
        if (customApplicationProperties == null) {
            customApplicationProperties = new Properties();
        }
        try {
            try (InputStream in = ClassLoader.getSystemResourceAsStream(resourceName)) {
                customApplicationProperties.load(in);
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Could not load resource: '" + resourceName + "'");
        }
    }

    public QuarkusProdModeTest overrideConfigKey(final String propertyKey, final String propertyValue) {
        if (customApplicationProperties == null) {
            customApplicationProperties = new Properties();
        }
        customApplicationProperties.put(propertyKey, propertyValue);
        return this;
    }

    public QuarkusProdModeTest setCommandLineParameters(String... commandLineParameters) {
        this.commandLineParameters = commandLineParameters;
        return this;
    }

    private static class PrintStackTraceTimerTask extends TimerTask {
        @Override
        public void run() {
            System.err.println("Test has been running for more than 5 minutes, thread dump is:");
            for (Map.Entry<Thread, StackTraceElement[]> i : Thread.getAllStackTraces().entrySet()) {
                System.err.println("\n");
                System.err.println(i.toString());
                System.err.println("\n");
                for (StackTraceElement j : i.getValue()) {
                    System.err.println(j);
                }
            }
        }
    }

    // the reason for using is this class is that we need to be able to copy the BuildStep into a new deployment archive that
    // is then added to the build
    public static class BuildChainCustomizerEntry {
        private final Class<? extends ProdModeTestBuildStep> buildStepClass;
        private final List<Class<? extends BuildItem>> produces;
        private final List<Class<? extends BuildItem>> consumes;

        public BuildChainCustomizerEntry(Class<? extends ProdModeTestBuildStep> buildStepClass,
                List<Class<? extends BuildItem>> produces,
                List<Class<? extends BuildItem>> consumes) {
            this.buildStepClass = Objects.requireNonNull(buildStepClass);
            this.produces = produces == null ? Collections.emptyList() : produces;
            this.consumes = consumes == null ? Collections.emptyList() : consumes;
        }

        public Class<? extends BuildStep> getBuildStepClass() {
            return buildStepClass;
        }

        public List<Class<? extends BuildItem>> getProduces() {
            return produces;
        }

        public List<Class<? extends BuildItem>> getConsumes() {
            return consumes;
        }
    }

}

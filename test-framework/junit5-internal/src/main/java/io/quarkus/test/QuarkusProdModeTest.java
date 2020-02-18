package io.quarkus.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.utilities.JavaBinFinder;

/**
 * A test extension for producing a prod-mode jar. This is meant to be used by extension authors, it's not intended for end user
 * consumption
 */
public class QuarkusProdModeTest
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final int DEFAULT_HTTP_PORT_INT = 8081;
    private static final String DEFAULT_HTTP_PORT = "" + DEFAULT_HTTP_PORT_INT;
    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    private Path outputDir;
    private Supplier<JavaArchive> archiveProducer;
    private String applicationName;
    private String applicationVersion;
    private boolean buildNative;

    private static final Timer timeoutTimer = new Timer("Test thread dump timer");
    private volatile TimerTask timeoutTask;
    private Properties customApplicationProperties;
    private CuratedApplication curatedApplication;

    private boolean run;

    private String logFileName;
    private Map<String, String> runtimeProperties;

    private Process process;

    private ProdModeTestResults prodModeTestResults;
    private Optional<Field> prodModeTestResultsField = Optional.empty();
    private Path logfilePath;
    private Optional<Field> logfileField = Optional.empty();

    public Supplier<JavaArchive> getArchiveProducer() {
        return archiveProducer;
    }

    public QuarkusProdModeTest setArchiveProducer(Supplier<JavaArchive> archiveProducer) {
        Objects.requireNonNull(archiveProducer);
        this.archiveProducer = archiveProducer;
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
     * The runtime configuration properties to be used if the built artifact is configured to be run
     */
    public QuarkusProdModeTest setRuntimeProperties(Map<String, String> runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
        return this;
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
        timeoutTask = new TimerTask() {
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
        };
        timeoutTimer.schedule(timeoutTask, 1000 * 60 * 5);

        Class<?> testClass = extensionContext.getRequiredTestClass();

        try {
            outputDir = Files.createTempDirectory("quarkus-prod-mode-test");
            Path deploymentDir = outputDir.resolve("deployment");
            Path buildDir = outputDir.resolve("build");

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
            try {
                QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder(deploymentDir)
                        .setMode(QuarkusBootstrap.Mode.PROD)
                        .setLocalProjectDiscovery(true)
                        .addExcludedPath(testLocation)
                        .setProjectRoot(testLocation)
                        .setTargetDirectory(buildDir);
                if (applicationName != null) {
                    builder.setBaseName(applicationName);
                }
                curatedApplication = builder.build().bootstrap();

                AugmentAction action = curatedApplication.createAugmentor();
                AugmentResult result = action.createProductionApplication();

                Path builtResultArtifact = setupProdModeResults(testClass, buildDir, result);

                if (run) {
                    startBuiltResult(builtResultArtifact);
                    RestAssuredURLManager.setURL(false,
                            runtimeProperties.get(QUARKUS_HTTP_PORT_PROPERTY) != null
                                    ? Integer.parseInt(runtimeProperties.get(QUARKUS_HTTP_PORT_PROPERTY))
                                    : DEFAULT_HTTP_PORT_INT);

                    if (logfilePath != null) {
                        logfileField = Arrays.stream(testClass.getDeclaredFields()).filter(
                                f -> f.isAnnotationPresent(LogFile.class) && Path.class.equals(f.getType()))
                                .findAny();
                        logfileField.ifPresent(f -> f.setAccessible(true));
                    }
                }

            } catch (Throwable e) {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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

        prodModeTestResults = new ProdModeTestResults(buildDir, builtResultArtifact, result.getResults());
        return builtResultArtifact;
    }

    private void startBuiltResult(Path builtResultArtifact) throws IOException {
        Path builtResultArtifactParentDir = builtResultArtifact.getParent();

        if (runtimeProperties == null) {
            runtimeProperties = new HashMap<>();
        } else {
            // copy the use supplied properties since it might an immutable map
            runtimeProperties = new HashMap<>(runtimeProperties);
        }
        runtimeProperties.putIfAbsent(QUARKUS_HTTP_PORT_PROPERTY, DEFAULT_HTTP_PORT);
        if (logFileName != null) {
            logfilePath = builtResultArtifactParentDir.resolve(logFileName);
            runtimeProperties.put("quarkus.log.file.path", logfilePath.toAbsolutePath().toString());
            runtimeProperties.put("quarkus.log.file.enable", "true");
        }
        List<String> systemProperties = runtimeProperties.entrySet().stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue()).collect(Collectors.toList());
        List<String> command = new ArrayList<>(systemProperties.size() + 3);
        if (builtResultArtifact.getFileName().toString().endsWith(".jar")) {
            command.add(JavaBinFinder.findBin());
            command.addAll(systemProperties);
            command.add("-jar");
            command.add(builtResultArtifact.toAbsolutePath().toString());
        } else {
            command.add(builtResultArtifact.toAbsolutePath().toString());
            command.addAll(systemProperties);
        }

        process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .directory(builtResultArtifactParentDir.toFile())
                .start();
        ensureApplicationStartupOrFailure();
    }

    private void ensureApplicationStartupOrFailure() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (true) {
            if (!process.isAlive()) {
                in.close();
                throw new RuntimeException(
                        "The produced jar could not be launched. Consult the above output for the exact cause.");
            }
            String line = in.readLine();
            if (line != null) {
                System.out.println(line);
                if (line.contains(EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED)) {
                    in.close();
                    break;
                }
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (run) {
            RestAssuredURLManager.clearURL();
        }

        try {
            if (process != null) {
                process.destroy();
                process.waitFor();
            }
        } catch (InterruptedException ignored) {

        }

        try {
            curatedApplication.close();
        } finally {
            timeoutTask.cancel();
            timeoutTask = null;

            if (outputDir != null) {
                Files.walkFileTree(outputDir, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (exc == null) {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        } else {
                            throw exc;
                        }
                    }
                });
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
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

    private static class PropertiesAsset implements Asset {
        private final Properties props;

        public PropertiesAsset(final Properties props) {
            this.props = props;
        }

        @Override
        public InputStream openStream() {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(128);
            try {
                props.store(outputStream, "Unit test Generated Application properties");
            } catch (IOException e) {
                throw new RuntimeException("Could not write application properties resource", e);
            }
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }
}

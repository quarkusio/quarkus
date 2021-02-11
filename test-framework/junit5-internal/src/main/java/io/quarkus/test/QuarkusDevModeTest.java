package io.quarkus.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

import org.jboss.logmanager.Logger;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.deployment.dev.CompilationProvider;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.DevModeMain;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.dev.appstate.ApplicationStateNotification;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.http.TestHTTPResourceManager;

/**
 * A test extension for testing Quarkus development mode in extensions. Intended for use by extension developers
 * testing their extension functionality in dev mode. Unlike {@link QuarkusUnitTest} this will test against
 * a clean deployment for each test method. This is nessesary to prevent undefined behaviour by making sure the
 * deployment starts in a clean state for each test.
 * <p>
 * <p>
 * NOTE: These tests do not run with {@link io.quarkus.runtime.LaunchMode#TEST} but rather with
 * {@link io.quarkus.runtime.LaunchMode#DEVELOPMENT}. This is necessary to ensure dev mode is tested correctly.
 * <p>
 * A side effect of this is that the tests will run on port 8080 by default instead of port 8081.
 */
public class QuarkusDevModeTest
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, TestInstanceFactory {

    private static final Logger rootLogger;
    private Handler[] originalRootLoggerHandlers;

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        java.util.logging.Logger logger = LogManager.getLogManager().getLogger("");
        if (!(logger instanceof org.jboss.logmanager.Logger)) {
            throw new IllegalStateException(
                    "QuarkusDevModeTest must be used with the the JBoss LogManager. See https://quarkus.io/guides/logging#how-to-configure-logging-for-quarkustest for an example of how to configure it in Maven.");
        }
        rootLogger = (org.jboss.logmanager.Logger) logger;
    }

    private DevModeMain devModeMain;
    private Path deploymentDir;
    private Supplier<JavaArchive> archiveProducer;
    private List<String> codeGenSources = Collections.emptyList();
    private String logFileName;
    private InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler((r) -> false);

    private Path deploymentSourceParentPath;
    private Path deploymentSourcePath;
    private Path deploymentResourcePath;
    private Path projectSourceRoot;
    private Path testLocation;
    private String[] commandLineArgs = new String[0];

    private static final List<CompilationProvider> compilationProviders;

    static {
        List<CompilationProvider> providers = new ArrayList<>();
        for (CompilationProvider provider : ServiceLoader.load(CompilationProvider.class)) {
            providers.add(provider);
        }
        compilationProviders = Collections.unmodifiableList(providers);
    }

    public Supplier<JavaArchive> getArchiveProducer() {
        return archiveProducer;
    }

    public QuarkusDevModeTest setArchiveProducer(Supplier<JavaArchive> archiveProducer) {
        this.archiveProducer = archiveProducer;
        return this;
    }

    public QuarkusDevModeTest setCodeGenSources(String... codeGenSources) {
        this.codeGenSources = Arrays.asList(codeGenSources);
        return this;
    }

    public QuarkusDevModeTest setLogFileName(String logFileName) {
        this.logFileName = logFileName;
        return this;
    }

    public QuarkusDevModeTest setLogRecordPredicate(Predicate<LogRecord> predicate) {
        this.inMemoryLogHandler = new InMemoryLogHandler(predicate);
        return this;
    }

    public List<LogRecord> getLogRecords() {
        return inMemoryLogHandler.records;
    }

    public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
            throws TestInstantiationException {
        try {
            Object actualTestInstance = factoryContext.getTestClass().newInstance();
            TestHTTPResourceManager.inject(actualTestInstance);
            return actualTestInstance;
        } catch (Exception e) {
            throw new TestInstantiationException("Unable to create test proxy", e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        //set the right launch mode in the outer CL, used by the HTTP host config source
        ProfileManager.setLaunchMode(LaunchMode.DEVELOPMENT);
        originalRootLoggerHandlers = rootLogger.getHandlers();
        rootLogger.addHandler(inMemoryLogHandler);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        if (archiveProducer == null) {
            throw new RuntimeException("QuarkusDevModeTest does not have archive producer set");
        }

        if (logFileName != null) {
            PropertyTestUtil.setLogFileProperty(logFileName);
        } else {
            PropertyTestUtil.setLogFileProperty();
        }
        ExtensionContext.Store store = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        if (store.get(TestResourceManager.class.getName()) == null) {
            TestResourceManager manager = new TestResourceManager(extensionContext.getRequiredTestClass());
            manager.init();
            manager.start();
            store.put(TestResourceManager.class.getName(), new ExtensionContext.Store.CloseableResource() {

                @Override
                public void close() throws Throwable {
                    manager.close();
                }
            });
        }

        Class<?> testClass = extensionContext.getRequiredTestClass();
        try {
            deploymentDir = Files.createTempDirectory("quarkus-dev-mode-test");
            testLocation = PathTestHelper.getTestClassesLocation(testClass);

            //TODO: this is a huge hack, at the moment this just guesses the source location
            //this can be improved, but as this is for testing extensions it is probably fine for now
            String sourcePath = System.getProperty("quarkus.test.source-path");
            if (sourcePath == null) {
                //TODO: massive hack, make sure this works in eclipse
                projectSourceRoot = testLocation.getParent().getParent().resolve("src/test/java");
            } else {
                projectSourceRoot = Paths.get(sourcePath);
            }
            // TODO: again a hack, assumes the sources dir is one dir above java sources path
            Path projectSourceParent = projectSourceRoot.getParent();

            DevModeContext context = exportArchive(deploymentDir, projectSourceRoot, projectSourceParent);
            context.setArgs(commandLineArgs);
            context.setTest(true);
            context.setAbortOnFailedStart(true);
            context.getBuildSystemProperties().put("quarkus.banner.enabled", "false");
            devModeMain = new DevModeMain(context);
            devModeMain.start();
            ApplicationStateNotification.waitForApplicationStart();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        rootLogger.setHandlers(originalRootLoggerHandlers);
        inMemoryLogHandler.clearRecords();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        try {
            if (devModeMain != null) {
                devModeMain.close();
                devModeMain = null;
            }
        } finally {
            if (deploymentDir != null) {
                FileUtil.deleteDirectory(deploymentDir);
            }
        }
        rootLogger.removeHandler(inMemoryLogHandler);
    }

    private DevModeContext exportArchive(Path deploymentDir, Path testSourceDir, Path testSourcesParentDir) {
        try {

            deploymentSourcePath = deploymentDir.resolve("src/main/java");
            deploymentSourceParentPath = deploymentDir.resolve("src/main");
            deploymentResourcePath = deploymentDir.resolve("src/main/resources");
            Path classes = deploymentDir.resolve("target/classes");
            Path targetDir = deploymentDir.resolve("target");
            Path cache = deploymentDir.resolve("target/dev-cache");
            Files.createDirectories(deploymentSourcePath);
            Files.createDirectories(deploymentResourcePath);
            Files.createDirectories(classes);
            Files.createDirectories(cache);

            //first we export the archive
            //then we attempt to generate a source tree
            JavaArchive archive = archiveProducer.get();
            archive.as(ExplodedExporter.class).exportExplodedInto(classes.toFile());
            copyFromSource(testSourceDir, deploymentSourcePath, classes);
            copyCodeGenSources(testSourcesParentDir, deploymentSourceParentPath, codeGenSources);

            //now copy resources
            //we assume everything that is not a .class file is a resource
            //resources are handled differently to sources as they are often not in the same location
            //in the FS, or are dynamically created
            try (Stream<Path> stream = Files.walk(classes)) {
                stream.forEach(s -> {
                    if (s.toString().endsWith(".class") ||
                            Files.isDirectory(s)) {
                        return;
                    }
                    String relative = classes.relativize(s).toString();
                    try {
                        try (InputStream in = Files.newInputStream(s)) {
                            byte[] data = FileUtil.readFileContents(in);
                            Path resolved = deploymentResourcePath.resolve(relative);
                            Files.createDirectories(resolved.getParent());
                            Files.write(resolved, data);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }

            //debugging code
            ExportUtil.exportToQuarkusDeploymentPath(archive);

            DevModeContext context = new DevModeContext();
            context.setCacheDir(cache.toFile());

            context.setApplicationRoot(
                    new DevModeContext.ModuleInfo(AppArtifactKey.fromString("io.quarkus.test:app-under-test"), "default",
                            deploymentDir.toAbsolutePath().toString(),
                            Collections.singleton(deploymentSourcePath.toAbsolutePath().toString()),
                            classes.toAbsolutePath().toString(), deploymentResourcePath.toAbsolutePath().toString(),
                            deploymentSourceParentPath.toAbsolutePath().toString(),
                            targetDir.resolve("generated-sources").toAbsolutePath().toString(),
                            targetDir.toAbsolutePath().toString()));

            setDevModeRunnerJarFile(context);
            return context;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create the archive", e);
        }
    }

    private void copyCodeGenSources(Path testSourcesParent, Path deploymentSourceParentPath, List<String> codeGenSources) {
        for (String codeGenDirName : codeGenSources) {
            Path codeGenSource = testSourcesParent.resolve(codeGenDirName);
            try {
                Path target = deploymentSourceParentPath.resolve(codeGenDirName);
                try (Stream<Path> files = Files.walk(codeGenSource)) {
                    files.forEach(
                            file -> {
                                Path targetPath = target.resolve(codeGenSource.relativize(file));
                                try {
                                    Files.copy(file, targetPath);
                                } catch (IOException e) {
                                    throw new RuntimeException(
                                            "Failed to copy file : " + file + " to " + targetPath.toAbsolutePath().toString());
                                }
                            });
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy code gen directory", e);
            }
        }
    }

    private static void setDevModeRunnerJarFile(final DevModeContext context) {
        handleSurefire(context);
        if (context.getDevModeRunnerJarFile() == null) {
            handleIntelliJ(context);
        }
    }

    /*
     * See https://github.com/quarkusio/quarkus/issues/6280
     * Maven surefire plugin launches the (forked) JVM for tests using a "surefirebooter" jar file.
     * This jar file's name starts with the prefix "surefirebooter" and ends with the extension ".jar".
     * The jar is launched using "java -jar .../surefirebooter*.jar ..." semantics. This jar has a
     * MANIFEST which contains "Class-Path" entries. These entries trigger a bug in the JDK code
     * https://bugs.openjdk.java.net/browse/JDK-8232170 which causes hot deployment related logic in Quarkus
     * to fail in dev mode.
     * The goal in this next section is to narrow down to this specific surefirebooter*.jar which was used to launch
     * the tests and mark it as the "dev mode runner jar" (through DevModeContext#setDevModeRunnerJarFile),
     * so that programmatic compilation of code (during hot deployment) doesn't run into issues noted in
     * https://bugs.openjdk.java.net/browse/JDK-8232170.
     * In reality the surefirebooter*.jar isn't really a "dev mode runner jar" (i.e. it's not the -dev.jar that
     * Quarkus generates), but it's fine to mark it as such to get past this issue. This is more of a workaround
     * on top of another workaround. In the medium/long term the actual JDK issue fix will make its way into
     * almost all prominently used Java versions.
     */
    private static void handleSurefire(DevModeContext context) {
        try {
            final Enumeration<URL> manifests = QuarkusDevModeTest.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (manifests.hasMoreElements()) {
                final URL url = manifests.nextElement();
                // don't open streams to manifest entries unless it resembles to the one
                // we are interested in
                if (!url.getPath().contains("surefirebooter")) {
                    continue;
                }
                final boolean foundForkedBooter = ClassPathUtils.readStream(url, is -> {
                    try {
                        final Manifest manifest = new Manifest(is);
                        final String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                        // additional check to make sure we are probing the right jar
                        if ("org.apache.maven.surefire.booter.ForkedBooter".equals(mainClass)) {
                            final String manifestFilePath = url.getPath();
                            if (manifestFilePath.startsWith("file:")) {
                                // manifest file path will be of the form jar:file:....!META-INF/MANIFEST.MF
                                final String jarFilePath = manifestFilePath.substring(5, manifestFilePath.lastIndexOf('!'));
                                final File surefirebooterJar = new File(
                                        URLDecoder.decode(jarFilePath, StandardCharsets.UTF_8.name()));
                                context.setDevModeRunnerJarFile(surefirebooterJar);
                            }
                            return true;
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    return false;
                });
                if (foundForkedBooter) {
                    break;
                }
            }
        } catch (Throwable ignored) {

        }
    }

    /*
     * IntelliJ does not create a special jar when running the tests but instead sets up the classpath and uses
     * the main class com.intellij.rt.junit.JUnitStarter from idea_rt.jar.
     * To make DevModeMain happy in this case, all we need to do here is create a dummy jar file in the proper directory
     */
    private static void handleIntelliJ(DevModeContext context) {
        try {
            final Enumeration<URL> manifests = QuarkusDevModeTest.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (manifests.hasMoreElements()) {
                final URL url = manifests.nextElement();
                if (!url.getPath().contains("idea_rt.jar")) {
                    continue;
                }

                Path intelliJPath = Paths.get(context.getApplicationRoot().getClassesPath()).getParent().resolve("intellij");
                Path dummyJar = intelliJPath.resolve("dummy.jar");

                // create the empty dummy jar
                try (FileSystem out = ZipUtils.newZip(dummyJar)) {

                }

                context.setDevModeRunnerJarFile(dummyJar.toFile());
                break;
            }
        } catch (Throwable ignored) {

        }
    }

    /**
     * Modifies a source file.
     *
     * @param sourceFile The unqualified name of the source file to modify
     * @param mutator A function that will modify the source code
     */
    public void modifySourceFile(String sourceFile, Function<String, String> mutator) {
        modifyFile(sourceFile, mutator, deploymentSourcePath);
    }

    /**
     * Modifies a file
     *
     * @param file file path relative to the project's sources parent dir (`src/main` for Maven)
     * @param mutator A function that will modify the file
     */
    public void modifyFile(String file, Function<String, String> mutator) {
        modifyPath(mutator, deploymentSourceParentPath, deploymentSourceParentPath.resolve(file));
    }

    /**
     * Modifies a source file.
     *
     * @param sourceFile The Class corresponding to the source file to modify
     * @param mutator A function that will modify the source code
     */
    public void modifySourceFile(Class<?> sourceFile, Function<String, String> mutator) {
        modifyFile(sourceFile.getSimpleName() + ".java", mutator, deploymentSourcePath);
    }

    /**
     * Adds the source file that corresponds to the given class to the deployment
     *
     * @param sourceFile
     */
    public void addSourceFile(Class<?> sourceFile) {
        Path path = copySourceFilesForClass(projectSourceRoot, deploymentSourcePath, testLocation,
                testLocation.resolve(sourceFile.getName().replace(".", "/") + ".class"));
        sleepForFileChanges(path);
        // since this is a new file addition, even wait for the parent dir's last modified timestamp to change
        sleepForFileChanges(path.getParent());
    }

    public String[] getCommandLineArgs() {
        return commandLineArgs;
    }

    public QuarkusDevModeTest setCommandLineArgs(String[] commandLineArgs) {
        this.commandLineArgs = commandLineArgs;
        return this;
    }

    void modifyFile(String name, Function<String, String> mutator, Path path) {
        AtomicBoolean found = new AtomicBoolean(false);
        try (Stream<Path> sources = Files.walk(path)) {
            sources.forEach(s -> {
                if (s.endsWith(name)) {
                    found.set(true);
                    modifyPath(mutator, path, s);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (!found.get()) {
            throw new IllegalArgumentException("File " + name + " was not part of the test application");
        }
    }

    private void modifyPath(Function<String, String> mutator, Path sourceDirectory, Path input) {
        try {
            byte[] data;
            try (InputStream in = Files.newInputStream(input)) {
                data = FileUtil.readFileContents(in);

            }
            String oldContent = new String(data, StandardCharsets.UTF_8);
            String content = mutator.apply(oldContent);
            if (content.equals(oldContent)) {
                throw new RuntimeException("File was not modified, mutator function had no effect");
            }

            sleepForFileChanges(sourceDirectory);
            Files.write(input, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void sleepForFileChanges(Path path) {
        try {
            //we want to make sure the last modified time is larger than both the current time
            //and the current last modified time. Some file systems only resolve file
            //time to the nearest second, so this is necessary for dev mode to pick up the changes
            long timeToBeat = Math.max(System.currentTimeMillis(), Files.getLastModifiedTime(path).toMillis());
            for (;;) {
                Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
                long fm = Files.getLastModifiedTime(path).toMillis();
                Thread.sleep(10);
                if (fm > timeToBeat) {
                    return;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds or overwrites a resource file with the given data. The path is an absolute path into to
     * the deployment resources directory
     */
    public void modifyResourceFile(String path, Function<String, String> mutator) {
        try {
            Path resourcePath = deploymentResourcePath.resolve(path);
            byte[] data;
            try (InputStream in = Files.newInputStream(resourcePath)) {
                data = FileUtil.readFileContents(in);

            }
            String content = new String(data, StandardCharsets.UTF_8);
            content = mutator.apply(content);

            Files.write(resourcePath, content.getBytes(StandardCharsets.UTF_8));
            sleepForFileChanges(resourcePath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Adds or overwrites a resource file with the given data. The path is an absolute path into to
     * the deployment resources directory
     */
    public void addResourceFile(String path, byte[] data) {
        final Path resourceFilePath = deploymentResourcePath.resolve(path);
        try {
            Files.write(resourceFilePath, data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        sleepForFileChanges(resourceFilePath);
        // since this is a new file addition, even wait for the parent dir's last modified timestamp to change
        sleepForFileChanges(resourceFilePath.getParent());
    }

    /**
     * Deletes a resource file. The path is an absolute path into to
     * the deployment resources directory
     */
    public void deleteResourceFile(String path) {
        final Path resourceFilePath = deploymentResourcePath.resolve(path);
        long timeout = System.currentTimeMillis() + 5000;
        //in general there is a potential race here
        //if you serve a file you will send the data to the client, then close the resource
        //this means that by the time the client request is run the file may not
        //have been closed yet, as the test sees the response as being complete after the last data is send
        //we wait up to 5s for this condition to be resolved
        for (;;) {
            try {
                Files.delete(resourceFilePath);
                break;
            } catch (IOException e) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    //ignore
                }
                if (System.currentTimeMillis() < timeout) {
                    continue;
                }
                throw new UncheckedIOException(e);
            }
        }
        // wait for last modified time of the parent to get updated
        sleepForFileChanges(resourceFilePath.getParent());
    }

    /**
     * Adds or overwrites a resource file with the given data encoded into UTF-8. The path is an absolute path into to
     * the deployment resources directory
     */
    public void addResourceFile(String path, String data) {
        addResourceFile(path, data.getBytes(StandardCharsets.UTF_8));
    }

    private void copyFromSource(Path projectSourcesDir, Path deploymentSourcesDir, Path classesDir)
            throws IOException {
        try (Stream<Path> classes = Files.walk(classesDir)) {
            classes.forEach((c) -> {

                if (Files.isDirectory(c) || !c.toString().endsWith(".class")) {
                    return;
                }
                copySourceFilesForClass(projectSourcesDir, deploymentSourcesDir, classesDir, c);
            });
        }
    }

    private Path copySourceFilesForClass(Path projectSourcesDir, Path deploymentSourcesDir, Path classesDir, Path classFile) {
        for (CompilationProvider provider : compilationProviders) {
            Path source = provider.getSourcePath(classFile,
                    Collections.singleton(projectSourcesDir.toAbsolutePath().toString()),
                    classesDir.toAbsolutePath().toString());
            if (source != null) {
                String relative = projectSourcesDir.relativize(source).toString();
                try (InputStream in = Files.newInputStream(source)) {
                    byte[] data = FileUtil.readFileContents(in);
                    Path resolved = deploymentSourcesDir.resolve(relative);
                    Files.createDirectories(resolved.getParent());
                    Files.write(resolved, data);
                    return resolved;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return null;
    }

}

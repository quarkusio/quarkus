package io.quarkus.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.dev.CompilationProvider;
import io.quarkus.dev.DevModeContext;
import io.quarkus.dev.DevModeMain;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.http.TestHTTPResourceManager;

/**
 * A test extension for testing Quarkus development mode in extensions. Intended for use by extension developers
 * testing their extension functionality in dev mode. Unlike {@link QuarkusUnitTest} this will test against
 * a clean deployment for each test method. This is nessesary to prevent undefined behaviour by making sure the
 * deployment starts in a clean state for each test.
 *
 *
 * NOTE: These tests do not run with {@link io.quarkus.runtime.LaunchMode#TEST} but rather with
 * {@link io.quarkus.runtime.LaunchMode#DEVELOPMENT}. This is necessary to ensure dev mode is tested correctly.
 *
 * A side effect of this is that the tests will run on port 8080 by default instead of port 8081.
 *
 */
public class QuarkusDevModeTest
        implements BeforeEachCallback, AfterEachCallback, TestInstanceFactory {

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    boolean started = false;

    private DevModeMain devModeMain;
    private Path deploymentDir;
    private Supplier<JavaArchive> archiveProducer;
    private String logFileName;

    private Path deploymentSourcePath;
    private Path deploymentResourcePath;
    private Path projectSourceRoot;
    private Path testLocation;

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

    public QuarkusDevModeTest setLogFileName(String logFileName) {
        this.logFileName = logFileName;
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
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
            manager.start();
            store.put(TestResourceManager.class.getName(), new ExtensionContext.Store.CloseableResource() {

                @Override
                public void close() throws Throwable {
                    manager.stop();
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
            ;
            if (sourcePath == null) {
                //TODO: massive hack, make sure this works in eclipse
                projectSourceRoot = testLocation.getParent().getParent().resolve("src/test/java");
            } else {
                projectSourceRoot = Paths.get(sourcePath);
            }

            DevModeContext context = exportArchive(deploymentDir, projectSourceRoot);
            context.setTest(true);
            context.setAbortOnFailedStart(true);
            devModeMain = new DevModeMain(context);
            devModeMain.start();
            started = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                Files.walkFileTree(deploymentDir, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            //ignore, this will be cleaned up on process exit anyway
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        try {
                            Files.delete(dir);
                        } catch (IOException e) {
                            //ignore, this will be cleaned up on process exit anyway
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    private DevModeContext exportArchive(Path deploymentDir, Path testSourceDir) {
        try {

            deploymentSourcePath = deploymentDir.resolve("src/main/java");
            deploymentResourcePath = deploymentDir.resolve("src/main/resources");
            Path classes = deploymentDir.resolve("target/classes");
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
                            try (OutputStream out = Files.newOutputStream(resolved)) {
                                out.write(data);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            //debugging code
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

            DevModeContext context = new DevModeContext();
            context.setCacheDir(cache.toFile());
            context.getClassesRoots().add(classes.toFile());

            context.getModules()
                    .add(new DevModeContext.ModuleInfo("default", deploymentDir.toAbsolutePath().toString(),
                            Collections.singleton(deploymentSourcePath.toAbsolutePath().toString()),
                            classes.toAbsolutePath().toString(), deploymentResourcePath.toAbsolutePath().toString()));

            return context;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create the archive", e);
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
        sleepForFileChanges();
        copySourceFilesForClass(projectSourceRoot, deploymentSourcePath, testLocation,
                testLocation.resolve(sourceFile.getName().replace(".", "/") + ".class"));
    }

    public void modifyFile(String name, Function<String, String> mutator, Path path) {
        try (Stream<Path> sources = Files.walk(path)) {
            sources.forEach(s -> {
                if (s.endsWith(name)) {
                    try {
                        byte[] data;
                        try (InputStream in = Files.newInputStream(s)) {
                            data = FileUtil.readFileContents(in);

                        }
                        String content = new String(data, StandardCharsets.UTF_8);
                        content = mutator.apply(content);

                        sleepForFileChanges();
                        try (OutputStream out = Files.newOutputStream(s)) {
                            out.write(content.getBytes(StandardCharsets.UTF_8));
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void sleepForFileChanges() {
        try {
            //this sucks, but some file systems only resolve last modified times to the second granularity
            //we need to sleep here before modification to make sure the detector actually picks up the change
            //we sleep to the nearest whole second plus two ms
            Thread.sleep(2002 - (System.currentTimeMillis() % 2000));
        } catch (InterruptedException e) {
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

            try (OutputStream out = Files.newOutputStream(resourcePath)) {
                out.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds or overwrites a resource file with the given data. The path is an absolute path into to
     * the deployment resources directory
     */
    public void addResourceFile(String path, byte[] data) {
        try {
            try (OutputStream out = Files.newOutputStream(deploymentResourcePath.resolve(path))) {
                out.write(data);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a resource file. The path is an absolute path into to
     * the deployment resources directory
     */
    public void deleteResourceFile(String path) {
        try {
            Files.delete(deploymentResourcePath.resolve(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private void copySourceFilesForClass(Path projectSourcesDir, Path deploymentSourcesDir, Path classesDir, Path classFile) {
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
                    try (OutputStream out = Files.newOutputStream(resolved)) {
                        out.write(data);
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }
    }

}

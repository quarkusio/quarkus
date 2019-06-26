package io.quarkus.creator.phase.augment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.builder.BuildResult;
import io.quarkus.creator.AppCreationPhase;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.config.reader.MappedPropertiesHandler;
import io.quarkus.creator.config.reader.PropertiesHandler;
import io.quarkus.creator.outcome.OutcomeProviderRegistration;
import io.quarkus.creator.phase.curate.CurateOutcome;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.ApplicationInfoUtil;
import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.QuarkusAugmentor;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateOutputBuildItem;
import io.quarkus.gizmo.NullWriter;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * This phase consumes {@link io.quarkus.creator.phase.curate.CurateOutcome} and processes
 * user application and its dependency classes for phases that generate a runnable application.
 *
 * @author Alexey Loubyansky
 */
public class AugmentPhase implements AppCreationPhase<AugmentPhase>, AugmentOutcome {

    private static final Logger log = Logger.getLogger(AugmentPhase.class);
    private static final String APPLICATION_INFO_PROPERTIES = "application-info.properties";
    private static final String META_INF = "META-INF";

    private Path outputDir;
    private Path appClassesDir;
    private Path transformedClassesDir;
    private Path wiringClassesDir;
    private Path generatedSourcesDir;
    private Path configDir;
    private Map<Path, Set<String>> transformedClassesByJar;

    /**
     * Output directory for the outcome of this phase.
     * If not set by the user the work directory of the creator
     * will be used instead.
     *
     * @param outputDir output directory for this phase
     * @return this phase instance
     */
    public AugmentPhase setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    /**
     * Directory containing application classes. If none is set by the user,
     * the creation process has to be initiated with an application JAR which
     * will be unpacked into classes directory in the creator's work directory.
     *
     * @param appClassesDir directory for application classes
     * @return this phase instance
     */
    public AugmentPhase setAppClassesDir(Path appClassesDir) {
        this.appClassesDir = appClassesDir;
        return this;
    }

    /**
     * Directory containing transformed application classes. If none is set by
     * the user, transformed-classes directory will be created in the work
     * directory of the creator.
     *
     * @param transformedClassesDir directory for transformed application classes
     * @return this phase instance
     */
    public AugmentPhase setTransformedClassesDir(Path transformedClassesDir) {
        this.transformedClassesDir = transformedClassesDir;
        return this;
    }

    /**
     * The directory for generated classes. If none is set by the user,
     * wiring-classes directory will be created in the work directory of the creator.
     *
     * @param wiringClassesDir directory for generated classes
     * @return this phase instance
     */
    public AugmentPhase setWiringClassesDir(Path wiringClassesDir) {
        this.wiringClassesDir = wiringClassesDir;
        return this;
    }

    /**
     * Directory containing the configuration files.
     *
     * @param configDir directory the configuration files (application.properties)
     * @return this phase instance
     */
    public AugmentPhase setConfigDir(Path configDir) {
        this.configDir = configDir;
        return this;
    }

    /**
     * The directory for generated source files. If none is set, then
     * no source file generation will be done.
     *
     * @param generatedSourcesDir the directory for generated source files
     * @return this phase instance
     */
    public AugmentPhase setGeneratedSourcesDir(final Path generatedSourcesDir) {
        this.generatedSourcesDir = generatedSourcesDir;
        return this;
    }

    @Override
    public Path getAppClassesDir() {
        return appClassesDir;
    }

    @Override
    public Path getTransformedClassesDir() {
        return transformedClassesDir;
    }

    @Override
    public Path getWiringClassesDir() {
        return wiringClassesDir;
    }

    @Override
    public Path getConfigDir() {
        return configDir;
    }

    @Override
    public Map<Path, Set<String>> getTransformedClassesByJar() {
        return transformedClassesByJar;
    }

    @Override
    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
        registration.provides(AugmentOutcome.class);
    }

    @Override
    public void provideOutcome(AppCreator ctx) throws AppCreatorException {
        final CurateOutcome appState = ctx.resolveOutcome(CurateOutcome.class);

        outputDir = outputDir == null ? ctx.getWorkPath() : IoUtils.mkdirs(outputDir);

        if (appClassesDir == null) {
            appClassesDir = outputDir.resolve("classes");
            Path appJar = appState.getAppArtifact().getPath();
            try {
                ZipUtils.unzip(appJar, appClassesDir);
            } catch (IOException e) {
                throw new AppCreatorException("Failed to unzip " + appJar, e);
            }
            final Path metaInf = appClassesDir.resolve(META_INF);
            IoUtils.recursiveDelete(metaInf.resolve("maven"));
            IoUtils.recursiveDelete(metaInf.resolve("INDEX.LIST"));
            IoUtils.recursiveDelete(metaInf.resolve("MANIFEST.MF"));
            IoUtils.recursiveDelete(metaInf.resolve(APPLICATION_INFO_PROPERTIES));
        }

        ApplicationInfoUtil.writeApplicationInfoProperties(appState.getAppArtifact(), appClassesDir);

        //lets default to appClassesDir for now
        if (configDir == null)
            configDir = appClassesDir;
        else {
            //if we use gradle we copy the configDir contents to appClassesDir
            try {
                if (!Files.isSameFile(configDir, appClassesDir)) {
                    Files.walkFileTree(configDir,
                            new CopyDirVisitor(configDir, appClassesDir, StandardCopyOption.REPLACE_EXISTING));
                }
            } catch (IOException e) {
                throw new AppCreatorException("Failed while copying files from " + configDir + " to " + appClassesDir, e);
            }
        }

        transformedClassesDir = IoUtils
                .mkdirs(transformedClassesDir == null ? outputDir.resolve("transformed-classes") : transformedClassesDir);
        wiringClassesDir = IoUtils.mkdirs(wiringClassesDir == null ? outputDir.resolve("wiring-classes") : wiringClassesDir);

        doProcess(appState);

        ctx.pushOutcome(AugmentOutcome.class, this);
    }

    private void doProcess(CurateOutcome appState) throws AppCreatorException {
        //first lets look for some config, as it is not on the current class path
        //and we need to load it to run the build process
        Path config = configDir.resolve("application.properties");
        if (Files.exists(config)) {
            try {
                Config built = SmallRyeConfigProviderResolver.instance().getBuilder()
                        .addDefaultSources()
                        .addDiscoveredConverters()
                        .addDiscoveredSources()
                        .withSources(new PropertiesConfigSource(config.toUri().toURL())).build();
                SmallRyeConfigProviderResolver.instance().registerConfig(built, Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        final AppModelResolver depResolver = appState.getArtifactResolver();
        List<AppDependency> appDeps;
        try {
            appDeps = appState.getEffectiveModel().getAllDependencies();
        } catch (BootstrapDependencyProcessingException e) {
            throw new AppCreatorException("Failed to resolve application build classpath", e);
        }

        URLClassLoader runnerClassLoader = null;
        try {
            // we need to make sure all the deployment artifacts are on the class path
            final List<URL> cpUrls = new ArrayList<>(appDeps.size() + 1);
            cpUrls.add(appClassesDir.toUri().toURL());

            for (AppDependency appDep : appDeps) {
                final Path resolvedDep = depResolver.resolve(appDep.getArtifact());
                cpUrls.add(resolvedDep.toUri().toURL());
            }

            runnerClassLoader = new URLClassLoader(cpUrls.toArray(new URL[cpUrls.size()]), getClass().getClassLoader());

            final Path wiringClassesDirectory = wiringClassesDir;
            ClassOutput classOutput = new ClassOutput() {
                @Override
                public void writeClass(boolean applicationClass, String className, byte[] data) throws IOException {
                    String location = className.replace('.', File.separatorChar);
                    final Path p = wiringClassesDirectory.resolve(location + ".class");
                    Files.createDirectories(p.getParent());
                    try (OutputStream out = Files.newOutputStream(p)) {
                        out.write(data);
                    }
                }

                @Override
                public void writeResource(String name, byte[] data) throws IOException {
                    final Path p = wiringClassesDirectory.resolve(name);
                    Files.createDirectories(p.getParent());
                    try (OutputStream out = Files.newOutputStream(p)) {
                        out.write(data);
                    }
                }

                @Override
                public Writer writeSource(final String className) {
                    String location = className.replace('.', '/');
                    final Path basePath = AugmentPhase.this.generatedSourcesDir;
                    if (basePath == null)
                        return NullWriter.INSTANCE;
                    final Path path = basePath.resolve("gizmo").resolve(location + ".zig");
                    try {
                        Files.createDirectories(path.getParent());
                        return Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to write source file", e);
                    }
                }
            };

            ClassLoader old = Thread.currentThread().getContextClassLoader();
            BuildResult result;
            try {
                Thread.currentThread().setContextClassLoader(runnerClassLoader);

                QuarkusAugmentor.Builder builder = QuarkusAugmentor.builder();
                builder.setRoot(appClassesDir);
                builder.setClassLoader(runnerClassLoader);
                builder.setOutput(classOutput);
                builder.addFinal(BytecodeTransformerBuildItem.class)
                        .addFinal(ApplicationArchivesBuildItem.class)
                        .addFinal(MainClassBuildItem.class)
                        .addFinal(SubstrateOutputBuildItem.class);
                result = builder.build().run();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }

            final List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems = result
                    .consumeMulti(BytecodeTransformerBuildItem.class);
            transformedClassesByJar = new HashMap<>();
            if (!bytecodeTransformerBuildItems.isEmpty()) {
                ApplicationArchivesBuildItem appArchives = result.consume(ApplicationArchivesBuildItem.class);

                final Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers = new HashMap<>(
                        bytecodeTransformerBuildItems.size());
                for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
                    bytecodeTransformers.computeIfAbsent(i.getClassToTransform(), (h) -> new ArrayList<>())
                            .add(i.getVisitorFunction());
                }

                // now copy all the contents to the runner jar
                // we also record if any additional archives needed transformation
                // when we copy these archives we will remove the problematic classes
                final ExecutorService executorPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                final ConcurrentLinkedDeque<Future<FutureEntry>> transformed = new ConcurrentLinkedDeque<>();
                try {
                    ClassLoader transformCl = runnerClassLoader;
                    for (Map.Entry<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> entry : bytecodeTransformers
                            .entrySet()) {
                        String className = entry.getKey();
                        ApplicationArchive archive = appArchives.containingArchive(entry.getKey());
                        if (archive != null) {
                            List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors = entry.getValue();
                            String classFileName = className.replace(".", "/") + ".class";
                            Path path = archive.getChildPath(classFileName);
                            transformedClassesByJar.computeIfAbsent(archive.getArchiveLocation(), (a) -> new HashSet<>())
                                    .add(classFileName);
                            transformed.add(executorPool.submit(new Callable<FutureEntry>() {
                                @Override
                                public FutureEntry call() throws Exception {
                                    ClassLoader old = Thread.currentThread().getContextClassLoader();
                                    try {
                                        Thread.currentThread().setContextClassLoader(transformCl);
                                        if (Files.size(path) > Integer.MAX_VALUE) {
                                            throw new RuntimeException(
                                                    "Can't process class files larger than Integer.MAX_VALUE bytes");
                                        }
                                        ClassReader cr = new ClassReader(Files.readAllBytes(path));
                                        ClassWriter writer = new QuarkusClassWriter(cr,
                                                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                                        ClassVisitor visitor = writer;
                                        for (BiFunction<String, ClassVisitor, ClassVisitor> i : visitors) {
                                            visitor = i.apply(className, visitor);
                                        }
                                        cr.accept(visitor, 0);
                                        return new FutureEntry(writer.toByteArray(), classFileName);
                                    } finally {
                                        Thread.currentThread().setContextClassLoader(old);
                                    }
                                }
                            }));
                        } else {
                            log.warnf("Cannot transform %s as it's containing application archive could not be found.",
                                    entry.getKey());
                        }
                    }

                } finally {
                    executorPool.shutdown();
                }
                if (!transformed.isEmpty()) {
                    for (Future<FutureEntry> i : transformed) {
                        final FutureEntry res = i.get();
                        final Path classFile = transformedClassesDir.resolve(res.location);
                        Files.createDirectories(classFile.getParent());
                        try (OutputStream out = Files.newOutputStream(classFile)) {
                            IoUtils.copy(out, new ByteArrayInputStream(res.data));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new AppCreatorException("Failed to augment application classes", e);
        } finally {
            if (runnerClassLoader != null) {
                try {
                    runnerClassLoader.close();
                } catch (IOException e) {
                    log.warn("Failed to close runner classloader", e);
                }
            }
        }
    }

    private static final class FutureEntry {
        final byte[] data;
        final String location;

        private FutureEntry(byte[] data, String location) {
            this.data = data;
            this.location = location;
        }
    }

    @Override
    public String getConfigPropertyName() {
        return "augment";
    }

    @Override
    public PropertiesHandler<AugmentPhase> getPropertiesHandler() {
        return new MappedPropertiesHandler<AugmentPhase>() {
            @Override
            public AugmentPhase getTarget() {
                return AugmentPhase.this;
            }
        }
                .map("output", (AugmentPhase t, String value) -> t.setOutputDir(Paths.get(value)))
                .map("classes", (AugmentPhase t, String value) -> t.setAppClassesDir(Paths.get(value)))
                .map("transformed-classes", (AugmentPhase t, String value) -> t.setTransformedClassesDir(Paths.get(value)))
                .map("wiring-classes", (AugmentPhase t, String value) -> t.setWiringClassesDir(Paths.get(value)))
                .map("config", (AugmentPhase t, String value) -> t.setConfigDir(Paths.get(value)));
    }

    public static class CopyDirVisitor extends SimpleFileVisitor<Path> {
        private final Path fromPath;
        private final Path toPath;
        private final CopyOption copyOption;

        public CopyDirVisitor(Path fromPath, Path toPath, CopyOption copyOption) {
            this.fromPath = fromPath;
            this.toPath = toPath;
            this.copyOption = copyOption;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetPath = toPath.resolve(fromPath.relativize(dir));
            if (!Files.exists(targetPath)) {
                Files.createDirectory(targetPath);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, toPath.resolve(fromPath.relativize(file)), copyOption);
            return FileVisitResult.CONTINUE;
        }
    }
}

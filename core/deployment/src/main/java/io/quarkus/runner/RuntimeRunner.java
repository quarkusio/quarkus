package io.quarkus.runner;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.QuarkusAugmentor;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.runner.classloading.FileClassPathElement;
import io.quarkus.runner.classloading.FilteringClassPathElement;
import io.quarkus.runner.classloading.JarClassPathElement;
import io.quarkus.runner.classloading.MemoryClassPathElement;
import io.quarkus.runner.classloading.QuarkusClassLoader;
import io.quarkus.runtime.Application;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.runtime.logging.InitialConfigurator;

/**
 * Class that can be used to run quarkus directly, executing the build and runtime
 * steps in the same JVM
 */
public class RuntimeRunner implements Runnable, Closeable {

    private final Path target;
    private final ClassLoader loader;
    private final ClassOutput classOutput;
    private final TransformerTarget transformerTarget;
    private Closeable closeTask;
    private final List<Path> additionalArchives;
    private final List<Path> hotDeploymentPaths;
    private final Collection<Path> excludedFromIndexing;
    private final List<Consumer<BuildChainBuilder>> chainCustomizers;
    private final LaunchMode launchMode;
    private final LiveReloadBuildItem liveReloadState;
    private final Properties buildSystemProperties;
    private final boolean requireRuntimeClassLoader;

    public RuntimeRunner(Builder builder) {
        this.target = builder.target;
        this.additionalArchives = new ArrayList<>(builder.additionalArchives);
        this.excludedFromIndexing = builder.excludedFromIndexing;
        this.chainCustomizers = new ArrayList<>(builder.chainCustomizers);
        this.launchMode = builder.launchMode;
        this.liveReloadState = builder.liveReloadState;
        if (builder.classOutput == null) {
            requireRuntimeClassLoader = true;
            List<Path> allPaths = new ArrayList<>();
            allPaths.add(target);
            allPaths.addAll(builder.additionalHotDeploymentPaths);
            hotDeploymentPaths = allPaths;
            //we need a special class loader, but it is not the runtime CL
            QuarkusClassLoader.Builder clb = QuarkusClassLoader.builder("Deployment time class loader", builder.classLoader,
                    false);
            for (Path i : hotDeploymentPaths) {
                clb.addElement(new FileClassPathElement(i));
            }
            this.loader = clb.build();
            RuntimeClassOuput classOutput = new RuntimeClassOuput(builder.getWiringClassesDir());
            this.classOutput = classOutput;
            this.transformerTarget = classOutput;
        } else {
            hotDeploymentPaths = null;
            requireRuntimeClassLoader = false;
            this.classOutput = builder.classOutput;
            this.transformerTarget = builder.transformerTarget;
            this.loader = builder.classLoader;
        }
        this.buildSystemProperties = builder.buildSystemProperties;
    }

    @Override
    public void close() throws IOException {
        if (closeTask != null) {
            closeTask.close();
        }
    }

    @Override
    public void run() {
        ProfileManager.setLaunchMode(launchMode);
        try {
            QuarkusAugmentor.Builder builder = QuarkusAugmentor.builder();
            builder.setRoot(target);
            builder.setClassLoader(loader);
            builder.setLaunchMode(launchMode);
            if (liveReloadState != null) {
                builder.setLiveReloadState(liveReloadState);
            }
            for (Path i : additionalArchives) {
                builder.addAdditionalApplicationArchive(i);
            }
            builder.excludeFromIndexing(excludedFromIndexing);
            for (Consumer<BuildChainBuilder> i : chainCustomizers) {
                builder.addBuildChainCustomizer(i);
            }
            builder.addFinal(BytecodeTransformerBuildItem.class)
                    .addFinal(ApplicationClassNameBuildItem.class);

            BuildResult result = builder.build().run();
            List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems = result
                    .consumeMulti(BytecodeTransformerBuildItem.class);
            if (!bytecodeTransformerBuildItems.isEmpty()) {
                Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> functions = new HashMap<>();
                for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
                    functions.computeIfAbsent(i.getClassToTransform(), (f) -> new ArrayList<>()).add(i.getVisitorFunction());
                }

                transformerTarget.setTransformers(functions);
            }

            for (GeneratedClassBuildItem i : result.consumeMulti(GeneratedClassBuildItem.class)) {
                classOutput.writeClass(i.isApplicationClass(), i.getName(), i.getClassData());
            }
            for (GeneratedResourceBuildItem i : result.consumeMulti(GeneratedResourceBuildItem.class)) {
                classOutput.writeResource(i.getName(), i.getClassData());
            }
            ClassLoader tccl = loader;

            if (requireRuntimeClassLoader) {
                ApplicationArchivesBuildItem archives = result.consume(ApplicationArchivesBuildItem.class);
                List<Path> applicationArchivePaths = archives.getApplicationArchives().stream()
                        .map(ApplicationArchive::getArchiveLocation).collect(Collectors.toList());

                QuarkusClassLoader.Builder clb = QuarkusClassLoader.builder("Runtime Class Loader", loader, false);
                RuntimeClassOuput output = (RuntimeClassOuput) classOutput;
                //set up a class loader that can load the in memory classes
                //and also all the application classes
                clb
                        .addElement(new MemoryClassPathElement(output.resources))
                        .setBytecodeTransformers(output.transformers);
                for (Path i : hotDeploymentPaths) {
                    clb.addElement(new FilteringClassPathElement(new FileClassPathElement(i), output.frameworkClasses));
                }
                for (Path i : calculateTransformableArchives(applicationArchivePaths, output.transformers)) {
                    if (Files.isDirectory(i)) {
                        clb.addElement(new FilteringClassPathElement(new FileClassPathElement(i), output.frameworkClasses));
                    } else {
                        clb.addElement(new FilteringClassPathElement(new JarClassPathElement(i), output.frameworkClasses));
                    }
                }

                tccl = clb.build();
            }

            final Application application;
            final String className = result.consume(ApplicationClassNameBuildItem.class).getClassName();
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(tccl);
                Class<? extends Application> appClass;
                try {
                    // force init here
                    appClass = Class.forName(className, true, tccl).asSubclass(Application.class);
                } catch (Throwable t) {
                    // todo: dev mode expects run time config to be available immediately even if static init didn't complete.
                    try {
                        final Class<?> configClass = Class.forName(RunTimeConfigurationGenerator.CONFIG_CLASS_NAME, true,
                                tccl);
                        configClass.getDeclaredMethod(RunTimeConfigurationGenerator.C_CREATE_RUN_TIME_CONFIG.getName())
                                .invoke(null);
                    } catch (Throwable t2) {
                        t.addSuppressed(t2);
                    }
                    throw t;
                }
                application = appClass.newInstance();
                application.start(null);
            } finally {
                //existing code expects the class loader to leak
                //TODO: fix all this
                //Thread.currentThread().setContextClassLoader(old);
            }

            closeTask = new Closeable() {
                @Override
                public void close() {
                    application.stop();
                }
            };
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // if the log handler is not activated, activate it with a default configuration to flush the messages
            if (!InitialConfigurator.DELAYED_HANDLER.isActivated()) {
                InitialConfigurator.DELAYED_HANDLER.setHandlers(new Handler[] { InitialConfigurator.createDefaultHandler() });
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ClassLoader classLoader;
        private Path target;
        private Path frameworkClassesPath;
        private Path wiringClassesDir;
        private Path transformerCache;
        private LaunchMode launchMode = LaunchMode.NORMAL;
        private final List<Path> additionalArchives = new ArrayList<>();
        private Set<Path> excludedFromIndexing = Collections.emptySet();

        /**
         * additional classes directories that may be hot deployed
         */
        private final List<Path> additionalHotDeploymentPaths = new ArrayList<>();
        private final List<Consumer<BuildChainBuilder>> chainCustomizers = new ArrayList<>();
        private ClassOutput classOutput;
        private TransformerTarget transformerTarget;
        private LiveReloadBuildItem liveReloadState;
        private Properties buildSystemProperties;

        public Builder setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Builder setTarget(Path target) {
            this.target = target;
            return this;
        }

        public Builder setFrameworkClassesPath(Path frameworkClassesPath) {
            this.frameworkClassesPath = frameworkClassesPath;
            return this;
        }

        public Builder setWiringClassesDir(Path wiringClassesDir) {
            this.wiringClassesDir = wiringClassesDir;
            return this;
        }

        public Builder setTransformerCache(Path transformerCache) {
            this.transformerCache = transformerCache;
            return this;
        }

        public Builder addAdditionalArchive(Path additionalArchive) {
            this.additionalArchives.add(additionalArchive);
            return this;
        }

        public Builder addAdditionalHotDeploymentPath(Path additionalPath) {
            this.additionalHotDeploymentPaths.add(additionalPath);
            return this;
        }

        public Builder addAdditionalArchives(Collection<Path> additionalArchives) {
            this.additionalArchives.addAll(additionalArchives);
            return this;
        }

        public Builder addChainCustomizer(Consumer<BuildChainBuilder> chainCustomizer) {
            this.chainCustomizers.add(chainCustomizer);
            return this;
        }

        public Builder addChainCustomizers(Collection<Consumer<BuildChainBuilder>> chainCustomizer) {
            this.chainCustomizers.addAll(chainCustomizer);
            return this;
        }

        public Builder excludeFromIndexing(Path p) {
            if (excludedFromIndexing.isEmpty()) {
                excludedFromIndexing = new HashSet<>(1);
            }
            excludedFromIndexing.add(p);
            return this;
        }

        public Builder setLaunchMode(LaunchMode launchMode) {
            this.launchMode = launchMode;
            return this;
        }

        public Builder setClassOutput(ClassOutput classOutput) {
            this.classOutput = classOutput;
            return this;
        }

        public Builder setTransformerTarget(TransformerTarget transformerTarget) {
            this.transformerTarget = transformerTarget;
            return this;
        }

        public Builder setLiveReloadState(LiveReloadBuildItem liveReloadState) {
            this.liveReloadState = liveReloadState;
            return this;
        }

        public Builder setBuildSystemProperties(final Properties buildSystemProperties) {
            this.buildSystemProperties = buildSystemProperties;
            return this;
        }

        Path getWiringClassesDir() {
            if (wiringClassesDir != null) {
                return wiringClassesDir;
            }
            if (frameworkClassesPath != null && Files.isDirectory(frameworkClassesPath)) {
                return frameworkClassesPath;
            }
            return Paths.get("").normalize().resolve("target").resolve("test-classes");
        }

        public RuntimeRunner build() {
            final RuntimeRunner runtimeRunner = new RuntimeRunner(this);
            excludedFromIndexing = Collections.emptySet();
            return runtimeRunner;
        }
    }

    /**
     * This class is temporary, it must be deleted in the class loading refactor, it is only present for the initial
     * replacement of RuntimeClassLoader.
     */
    @Deprecated
    static class RuntimeClassOuput implements ClassOutput, TransformerTarget {

        static final Logger log = Logger.getLogger(RuntimeClassOuput.class);
        static final String DEBUG_CLASSES_DIR = System.getProperty("quarkus.debug.generated-classes-dir");
        final Map<String, byte[]> resources = new ConcurrentHashMap<>();
        final Set<String> frameworkClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());
        volatile Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> transformers;

        private final Path frameworkClassesPath;

        RuntimeClassOuput(Path frameworkClassesPath) {
            this.frameworkClassesPath = frameworkClassesPath;
        }

        @Override
        public void writeClass(boolean applicationClass, String className, byte[] data) throws IOException {
            if (applicationClass) {
                resources.put(className.replace(".", "/") + ".class", data);
                if (DEBUG_CLASSES_DIR != null) {
                    try {
                        File debugPath = new File(DEBUG_CLASSES_DIR);
                        if (!debugPath.exists()) {
                            debugPath.mkdir();
                        }
                        File classFile = new File(debugPath, className + ".class");
                        classFile.getParentFile().mkdirs();
                        try (FileOutputStream classWriter = new FileOutputStream(classFile)) {
                            classWriter.write(data);
                        }
                        log.infof("Wrote %s", classFile.getAbsolutePath());
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            } else {
                //this is pretty horrible
                //basically we add the framework level classes to the file system
                //in the same dir as the actual app classes
                //however as we add them to the frameworkClasses set we know to load them
                //from the parent CL
                frameworkClasses.add(className.replace('/', '.'));
                final Path fileName = frameworkClassesPath.resolve(className.replace('.', '/') + ".class");
                try {
                    Files.createDirectories(fileName.getParent());
                    try (FileOutputStream out = new FileOutputStream(fileName.toFile())) {
                        out.write(data);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void writeResource(String name, byte[] data) throws IOException {
            resources.put(name, data);
        }

        @Override
        public void setTransformers(Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> functions) {
            this.transformers = functions;
        }
    }

    //TODO: delete this
    private static List<Path> calculateTransformableArchives(List<Path> archives,
            Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers) {
        //we also need to be able to transform application archives
        //this is not great but I can't really see a better solution
        if (bytecodeTransformers == null) {
            return Collections.emptyList();
        }
        List<Path> ret = new ArrayList<>();
        try {
            for (Path root : archives) {
                Map<String, Path> classes = new HashMap<>();
                AtomicBoolean transform = new AtomicBoolean();
                try (Stream<Path> fileTreeElements = Files.walk(root)) {
                    fileTreeElements.forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path path) {
                            if (path.toString().endsWith(".class")) {
                                String key = root.relativize(path).toString().replace('\\', '/');
                                classes.put(key, path);
                                if (bytecodeTransformers
                                        .containsKey(key.substring(0, key.length() - ".class".length()).replace("/", "."))) {
                                    transform.set(true);
                                }
                            }
                        }
                    });
                }
                if (transform.get()) {
                    ret.add(root);
                }
            }
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

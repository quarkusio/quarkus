package io.quarkus.runner;

import java.io.Closeable;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.stream.Collectors;

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
    private final Collection<Path> excludedFromIndexing;
    private final List<Consumer<BuildChainBuilder>> chainCustomizers;
    private final LaunchMode launchMode;
    private final LiveReloadBuildItem liveReloadState;
    private final Properties buildSystemProperties;

    public RuntimeRunner(Builder builder) {
        this.target = builder.target;
        this.additionalArchives = new ArrayList<>(builder.additionalArchives);
        this.excludedFromIndexing = builder.excludedFromIndexing;
        this.chainCustomizers = new ArrayList<>(builder.chainCustomizers);
        this.launchMode = builder.launchMode;
        this.liveReloadState = builder.liveReloadState;
        if (builder.classOutput == null) {
            List<Path> allPaths = new ArrayList<>();
            allPaths.add(target);
            allPaths.addAll(builder.additionalHotDeploymentPaths);
            RuntimeClassLoader runtimeClassLoader = new RuntimeClassLoader(builder.classLoader, allPaths,
                    builder.getWiringClassesDir(), builder.transformerCache);
            this.loader = runtimeClassLoader;
            this.classOutput = runtimeClassLoader;
            this.transformerTarget = runtimeClassLoader;
        } else {
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
        Thread.currentThread().setContextClassLoader(loader);
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

            if (loader instanceof RuntimeClassLoader) {
                ApplicationArchivesBuildItem archives = result.consume(ApplicationArchivesBuildItem.class);
                ((RuntimeClassLoader) loader).setApplicationArchives(archives.getApplicationArchives().stream()
                        .map(ApplicationArchive::getArchiveRoot).collect(Collectors.toList()));
            }
            for (GeneratedClassBuildItem i : result.consumeMulti(GeneratedClassBuildItem.class)) {
                classOutput.writeClass(i.isApplicationClass(), i.getName(), i.getClassData());
            }
            for (GeneratedResourceBuildItem i : result.consumeMulti(GeneratedResourceBuildItem.class)) {
                classOutput.writeResource(i.getName(), i.getClassData());
            }

            final Application application;
            final String className = result.consume(ApplicationClassNameBuildItem.class).getClassName();
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(loader);
                Class<? extends Application> appClass;
                try {
                    // force init here
                    appClass = Class.forName(className, true, loader).asSubclass(Application.class);
                } catch (Throwable t) {
                    // todo: dev mode expects run time config to be available immediately even if static init didn't complete.
                    try {
                        final Class<?> configClass = Class.forName(RunTimeConfigurationGenerator.CONFIG_CLASS_NAME, true,
                                loader);
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
                Thread.currentThread().setContextClassLoader(old);
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
}

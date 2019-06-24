package io.quarkus.runner;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.objectweb.asm.ClassVisitor;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.QuarkusAugmentor;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.runtime.Application;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;

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
    private final List<Consumer<BuildChainBuilder>> chainCustomizers;
    private final LaunchMode launchMode;
    private final LiveReloadBuildItem liveReloadState;

    public RuntimeRunner(Builder builder) {
        this.target = builder.target;
        this.additionalArchives = new ArrayList<>(builder.additionalArchives);
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
            builder.setOutput(classOutput);
            builder.setLaunchMode(launchMode);
            if (liveReloadState != null) {
                builder.setLiveReloadState(liveReloadState);
            }
            for (Path i : additionalArchives) {
                builder.addAdditionalApplicationArchive(i);
            }
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

            final Application application;
            Class<? extends Application> appClass = loader
                    .loadClass(result.consume(ApplicationClassNameBuildItem.class).getClassName())
                    .asSubclass(Application.class);
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(loader);
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
        /**
         * additional classes directories that may be hot deployed
         */
        private final List<Path> additionalHotDeploymentPaths = new ArrayList<>();
        private final List<Consumer<BuildChainBuilder>> chainCustomizers = new ArrayList<>();
        private ClassOutput classOutput;
        private TransformerTarget transformerTarget;
        private LiveReloadBuildItem liveReloadState;

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
            return new RuntimeRunner(this);
        }
    }
}

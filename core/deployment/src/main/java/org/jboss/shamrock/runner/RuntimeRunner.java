/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.runner;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jboss.builder.BuildChainBuilder;
import org.jboss.builder.BuildResult;
import org.jboss.shamrock.deployment.ShamrockAugmentor;
import org.jboss.shamrock.deployment.builditem.ApplicationClassNameBuildItem;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.runtime.Application;
import org.jboss.shamrock.runtime.LaunchMode;
import org.objectweb.asm.ClassVisitor;

/**
 * Class that can be used to run shamrock directly, executing the build and runtime
 * steps in the same JVM
 */
public class RuntimeRunner implements Runnable, Closeable {

    private final Path target;
    private final RuntimeClassLoader loader;
    private Closeable closeTask;
    private final List<Path> additionalArchives;
    private final List<Consumer<BuildChainBuilder>> chainCustomizers;
    private final LaunchMode launchMode;

    public RuntimeRunner(Builder builder) {
        this.target = builder.target;
        this.additionalArchives = new ArrayList<>(builder.additionalArchives);
        this.chainCustomizers = new ArrayList<>(builder.chainCustomizers);
        this.launchMode = builder.launchMode;
        this.loader = new RuntimeClassLoader(builder.classLoader, target, builder.frameworkClassesPath, builder.transformerCache);
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
        try {
            ShamrockAugmentor.Builder builder = ShamrockAugmentor.builder();
            builder.setRoot(target);
            builder.setClassLoader(loader);
            builder.setOutput(loader);
            builder.setLaunchMode(launchMode);
            for (Path i : additionalArchives) {
                builder.addAdditionalApplicationArchive(i);
            }
            for (Consumer<BuildChainBuilder> i : chainCustomizers) {
                builder.addBuildChainCustomizer(i);
            }
            builder.addFinal(BytecodeTransformerBuildItem.class)
                    .addFinal(ApplicationClassNameBuildItem.class);

            BuildResult result = builder.build().run();
            List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems = result.consumeMulti(BytecodeTransformerBuildItem.class);
            if (!bytecodeTransformerBuildItems.isEmpty()) {
                Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> functions = new HashMap<>();
                for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
                    functions.computeIfAbsent(i.getClassToTransform(), (f) -> new ArrayList<>()).add(i.getVisitorFunction());
                }

                loader.setTransformers(functions);
            }


            final Application application;
            Class<? extends Application> appClass = loader.loadClass(result.consume(ApplicationClassNameBuildItem.class).getClassName()).asSubclass(Application.class);
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
        private Path transformerCache;
        private LaunchMode launchMode = LaunchMode.NORMAL;
        private final List<Path> additionalArchives = new ArrayList<>();
        private final List<Consumer<BuildChainBuilder>> chainCustomizers = new ArrayList<>();

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

        public Builder setTransformerCache(Path transformerCache) {
            this.transformerCache = transformerCache;
            return this;
        }

        public Builder addAdditionalArchive(Path additionalArchive) {
            this.additionalArchives.add(additionalArchive);
            return this;
        }
        public Builder addAdditionalArchives(Collection<Path> additionalArchive) {
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

        public LaunchMode getLaunchMode() {
            return launchMode;
        }

        public Builder setLaunchMode(LaunchMode launchMode) {
            this.launchMode = launchMode;
            return this;
        }

        public RuntimeRunner build() {
            return new RuntimeRunner(this);
        }
    }
}

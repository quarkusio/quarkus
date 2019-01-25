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
import org.objectweb.asm.ClassVisitor;
import org.wildfly.common.Assert;

/**
 * Class that can be used to run shamrock directly, executing the build and runtime
 * steps in the same JVM
 */
public class RuntimeRunner implements Runnable, Closeable {

    private final Path applicationRoot;
    private final RuntimeClassLoader loader;
    private Closeable closeTask;
    private final List<Path> additionalArchives;
    private List<Consumer<BuildChainBuilder>> chainCustomizers;

    RuntimeRunner(Builder builder) {
        this.applicationRoot = builder.applicationRoot;
        this.additionalArchives = new ArrayList<>(builder.additionalArchives);
        this.chainCustomizers = new ArrayList<>(builder.chainCustomizers);
        this.additionalArchives.addAll(builder.additionalApplicationRoots);
        RuntimeClassLoader rcl = new RuntimeClassLoader(builder.classLoader, applicationRoot, builder.frameworkClassesPath, builder.transformerCache, builder.additionalApplicationRoots);
        this.loader = rcl;
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
            builder.setRoot(applicationRoot);
            builder.setClassLoader(loader);
            builder.setOutput(loader);
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
        private Path applicationRoot;
        /**
         * additional roots for classes that need to be loaded into the runtime class loader
         *
         * e.g. in a multi module maven project this might be used to allow for hot deployment from changes in any module (TODO)
         *
         */
        private final List<Path> additionalApplicationRoots = new ArrayList<>();
        private Closeable closeTask;
        private final List<Path> additionalArchives = new ArrayList<>();
        private final List<Consumer<BuildChainBuilder>> chainCustomizers = new ArrayList<>();
        private Path frameworkClassesPath;
        private Path transformerCache;

        public Builder addChainCustomizer(Consumer<BuildChainBuilder> customizer) {
            chainCustomizers.add(customizer);
            return this;
        }

        public Builder addAdditionalArchive(Path archive) {
            additionalArchives.add(archive);
            return this;
        }

        public Builder addAdditionalArchives(Collection<Path> archives) {
            additionalArchives.addAll(archives);
            return this;
        }
        public Builder addAdditionalApplicationRoot(Path root) {
            additionalApplicationRoots.add(root);
            return this;
        }

        public Builder setCloseTask(Closeable closeTask) {
            this.closeTask = closeTask;
            return this;
        }

        public Builder setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Builder setApplicationRoot(Path applicationRoot) {
            this.applicationRoot = applicationRoot;
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

        public RuntimeRunner build() {
            Assert.checkNotNullParam("target", applicationRoot);
            Assert.checkNotNullParam("classLoader", classLoader);
            Assert.checkNotNullParam("frameworkClassesPath", frameworkClassesPath);
            return new RuntimeRunner(this);
        }
    }
}

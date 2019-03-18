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

package io.quarkus.deployment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.builder.BuildChain;
import org.jboss.builder.BuildChainBuilder;
import org.jboss.builder.BuildExecutionBuilder;
import org.jboss.builder.BuildResult;
import org.jboss.builder.item.BuildItem;
import org.jboss.logging.Logger;

import io.quarkus.deployment.builditem.AdditionalApplicationArchiveBuildItem;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.ClassOutputBuildItem;
import io.quarkus.deployment.builditem.ExtensionClassLoaderBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.runtime.LaunchMode;

public class QuarkusAugmentor {

    private static final Logger log = Logger.getLogger(QuarkusAugmentor.class);

    private final ClassOutput output;
    private final ClassLoader classLoader;
    private final Path root;
    private final Set<Class<? extends BuildItem>> finalResults;
    private final List<Consumer<BuildChainBuilder>> buildChainCustomizers;
    private final LaunchMode launchMode;
    private final List<Path> additionalApplicationArchives;

    QuarkusAugmentor(Builder builder) {
        this.output = builder.output;
        this.classLoader = builder.classLoader;
        this.root = builder.root;
        this.finalResults = new HashSet<>(builder.finalResults);
        this.buildChainCustomizers = new ArrayList<>(builder.buildChainCustomizers);
        this.launchMode = builder.launchMode;
        this.additionalApplicationArchives = new ArrayList<>(builder.additionalApplicationArchives);
    }

    public BuildResult run() throws Exception {
        long time = System.currentTimeMillis();
        log.info("Beginning quarkus augmentation");
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);

            final BuildChainBuilder chainBuilder = BuildChain.builder();

            ExtensionLoader.loadStepsFrom(classLoader).accept(chainBuilder);
            chainBuilder.loadProviders(classLoader);

            chainBuilder
                    .addInitial(QuarkusConfig.class)
                    .addInitial(ArchiveRootBuildItem.class)
                    .addInitial(ShutdownContextBuildItem.class)
                    .addInitial(ClassOutputBuildItem.class)
                    .addInitial(LaunchModeBuildItem.class)
                    .addInitial(AdditionalApplicationArchiveBuildItem.class)
                    .addInitial(ExtensionClassLoaderBuildItem.class);
            for (Class<? extends BuildItem> i : finalResults) {
                chainBuilder.addFinal(i);
            }
            chainBuilder.addFinal(GeneratedClassBuildItem.class)
                    .addFinal(GeneratedResourceBuildItem.class);

            for (Consumer<BuildChainBuilder> i : buildChainCustomizers) {
                i.accept(chainBuilder);
            }

            BuildChain chain = chainBuilder
                    .build();
            BuildExecutionBuilder execBuilder = chain.createExecutionBuilder("main")
                    .produce(QuarkusConfig.INSTANCE)
                    .produce(new ArchiveRootBuildItem(root))
                    .produce(new ClassOutputBuildItem(output))
                    .produce(new ShutdownContextBuildItem())
                    .produce(new LaunchModeBuildItem(launchMode))
                    .produce(new ExtensionClassLoaderBuildItem(classLoader));
            for (Path i : additionalApplicationArchives) {
                execBuilder.produce(new AdditionalApplicationArchiveBuildItem(i));
            }
            BuildResult buildResult = execBuilder
                    .execute();

            //TODO: this seems wrong
            for (GeneratedClassBuildItem i : buildResult.consumeMulti(GeneratedClassBuildItem.class)) {
                output.writeClass(i.isApplicationClass(), i.getName(), i.getClassData());
            }
            for (GeneratedResourceBuildItem i : buildResult.consumeMulti(GeneratedResourceBuildItem.class)) {
                output.writeResource(i.getName(), i.getClassData());
            }
            log.info("Quarkus augmentation completed in " + (System.currentTimeMillis() - time) + "ms");
            return buildResult;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        List<Path> additionalApplicationArchives = new ArrayList<>();
        ClassOutput output;
        ClassLoader classLoader;
        Path root;
        Set<Class<? extends BuildItem>> finalResults = new HashSet<>();
        private final List<Consumer<BuildChainBuilder>> buildChainCustomizers = new ArrayList<>();
        LaunchMode launchMode = LaunchMode.NORMAL;

        public Builder addBuildChainCustomizer(Consumer<BuildChainBuilder> customizer) {
            this.buildChainCustomizers.add(customizer);
            return this;
        }

        public List<Path> getAdditionalApplicationArchives() {
            return additionalApplicationArchives;
        }

        public Builder addAdditionalApplicationArchive(Path archive) {
            this.additionalApplicationArchives.add(archive);
            return this;
        }

        public ClassOutput getOutput() {
            return output;
        }

        public Builder setOutput(ClassOutput output) {
            this.output = output;
            return this;
        }

        public LaunchMode getLaunchMode() {
            return launchMode;
        }

        public Builder setLaunchMode(LaunchMode launchMode) {
            this.launchMode = launchMode;
            return this;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public Builder setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Path getRoot() {
            return root;
        }

        public <T extends BuildItem> Builder addFinal(Class<T> clazz) {
            finalResults.add(clazz);
            return this;
        }

        public Builder setRoot(Path root) {
            this.root = root;
            return this;
        }

        public QuarkusAugmentor build() {
            return new QuarkusAugmentor(this);
        }
    }
}

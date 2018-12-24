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

package org.jboss.shamrock.deployment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.builder.BuildChain;
import org.jboss.builder.BuildChainBuilder;
import org.jboss.builder.BuildContext;
import org.jboss.builder.BuildResult;
import org.jboss.builder.BuildStep;
import org.jboss.builder.item.BuildItem;
import org.jboss.logging.Logger;
import org.jboss.shamrock.deployment.builditem.ArchiveRootBuildItem;
import org.jboss.shamrock.deployment.builditem.ClassOutputBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.ShutdownContextBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;

public class ShamrockAugmentor {

    private static final Logger log = Logger.getLogger(ShamrockAugmentor.class);

    private final ClassOutput output;
    private final ClassLoader classLoader;
    private final Path root;
    private final Set<Class<? extends BuildItem>> finalResults;

    ShamrockAugmentor(Builder builder) {
        this.output = builder.output;
        this.classLoader = builder.classLoader;
        this.root = builder.root;
        this.finalResults = new HashSet<>(builder.finalResults);
    }

    public BuildResult run() throws Exception {
        long time = System.currentTimeMillis();
        log.info("Beginning shamrock augmentation");
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);

            BuildChainBuilder chainBuilder = BuildChain.builder()

                    .loadProviders(Thread.currentThread().getContextClassLoader())
                    .addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            //TODO: this should not be here
                            context.produce(new SubstrateResourceBuildItem("META-INF/microprofile-config.properties"));
                            context.produce(ShamrockConfig.INSTANCE);
                            context.produce(new ArchiveRootBuildItem(root));
                            context.produce(new ClassOutputBuildItem(output));
                            context.produce(new ShutdownContextBuildItem());
                        }
                    })
                    .produces(ShamrockConfig.class)
                    .produces(SubstrateResourceBuildItem.class)
                    .produces(ArchiveRootBuildItem.class)
                    .produces(ShutdownContextBuildItem.class)
                    .produces(ClassOutputBuildItem.class)
                    .build();
            for (Class<? extends BuildItem> i : finalResults) {
                chainBuilder.addFinal(i);
            }
            chainBuilder.addFinal(GeneratedClassBuildItem.class)
                    .addFinal(GeneratedResourceBuildItem.class);

            BuildChain chain = chainBuilder
                    .build();
            BuildResult buildResult = chain.createExecutionBuilder("main").execute();

            //TODO: this seems wrong
            for (GeneratedClassBuildItem i : buildResult.consumeMulti(GeneratedClassBuildItem.class)) {
                output.writeClass(i.isApplicationClass(), i.getName(), i.getClassData());
            }
            for (GeneratedResourceBuildItem i : buildResult.consumeMulti(GeneratedResourceBuildItem.class)) {
                output.writeResource(i.getName(), i.getClassData());
            }
            log.info("Shamrock augmentation completed in " + (System.currentTimeMillis() - time) + "ms");
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

        public ShamrockAugmentor build() {
            return new ShamrockAugmentor(this);
        }
    }
}

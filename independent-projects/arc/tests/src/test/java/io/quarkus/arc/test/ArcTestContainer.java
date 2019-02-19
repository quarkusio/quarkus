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

package org.jboss.quarkus.arc.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.quarkus.arc.Arc;
import org.jboss.quarkus.arc.ComponentsProvider;
import org.jboss.quarkus.arc.ResourceReferenceProvider;
import org.jboss.quarkus.arc.processor.AnnotationsTransformer;
import org.jboss.quarkus.arc.processor.BeanDeploymentValidator;
import org.jboss.quarkus.arc.processor.BeanInfo;
import org.jboss.quarkus.arc.processor.BeanProcessor;
import org.jboss.quarkus.arc.processor.BeanRegistrar;
import org.jboss.quarkus.arc.processor.DeploymentEnhancer;
import org.jboss.quarkus.arc.processor.ResourceOutput;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ArcTestContainer implements TestRule {

    private static final String TARGET_TEST_CLASSES = "target/test-classes";

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final List<Class<?>> resourceReferenceProviders;
        private final List<Class<?>> beanClasses;
        private final List<Class<? extends Annotation>> resourceAnnotations;
        private final List<BeanRegistrar> beanRegistrars;
        private final List<AnnotationsTransformer> annotationsTransformers;
        private final List<DeploymentEnhancer> deploymentEnhancers;
        private final List<BeanDeploymentValidator> beanDeploymentValidators;
        private boolean shouldFail = false;
        private boolean removeUnusedBeans = false;
        private final List<Predicate<BeanInfo>> exclusions;

        public Builder() {
            resourceReferenceProviders = new ArrayList<>();
            beanClasses = new ArrayList<>();
            resourceAnnotations = new ArrayList<>();
            beanRegistrars = new ArrayList<>();
            annotationsTransformers = new ArrayList<>();
            deploymentEnhancers = new ArrayList<>();
            beanDeploymentValidators = new ArrayList<>();
            exclusions = new ArrayList<>();
        }

        public Builder resourceReferenceProviders(Class<?>... resourceReferenceProviders) {
            Collections.addAll(this.resourceReferenceProviders, resourceReferenceProviders);
            return this;
        }

        public Builder beanClasses(Class<?>... beanClasses) {
            Collections.addAll(this.beanClasses, beanClasses);
            return this;
        }

        @SafeVarargs
        public final Builder resourceAnnotations(Class<? extends Annotation>... resourceAnnotations) {
            Collections.addAll(this.resourceAnnotations, resourceAnnotations);
            return this;
        }

        public Builder beanRegistrars(BeanRegistrar... registrars) {
            Collections.addAll(this.beanRegistrars, registrars);
            return this;
        }

        public Builder annotationsTransformers(AnnotationsTransformer... transformers) {
            Collections.addAll(this.annotationsTransformers, transformers);
            return this;
        }

        public Builder deploymentEnhancers(DeploymentEnhancer... enhancers) {
            Collections.addAll(this.deploymentEnhancers, enhancers);
            return this;
        }

        public Builder beanDeploymentValidators(BeanDeploymentValidator... validators) {
            Collections.addAll(this.beanDeploymentValidators, validators);
            return this;
        }

        public Builder removeUnusedBeans(boolean value) {
            this.removeUnusedBeans = value;
            return this;
        }

        public Builder addRemovalExclusion(Predicate<BeanInfo> exclusion) {
            this.exclusions.add(exclusion);
            return this;
        }

        public Builder shouldFail() {
            this.shouldFail = true;
            return this;
        }

        public ArcTestContainer build() {
            return new ArcTestContainer(resourceReferenceProviders, beanClasses, resourceAnnotations, beanRegistrars, annotationsTransformers,
                    deploymentEnhancers, beanDeploymentValidators, shouldFail, removeUnusedBeans, exclusions);
        }

    }

    private final List<Class<?>> resourceReferenceProviders;

    private final List<Class<?>> beanClasses;

    private final List<Class<? extends Annotation>> resourceAnnotations;

    private final List<BeanRegistrar> beanRegistrars;

    private final List<AnnotationsTransformer> annotationsTransformers;

    private final List<DeploymentEnhancer> deploymentEnhancers;

    private final List<BeanDeploymentValidator> beanDeploymentValidators;

    private final boolean shouldFail;
    private final AtomicReference<Throwable> buildFailure;

    private final boolean removeUnusedBeans;
    private final List<Predicate<BeanInfo>> exclusions;

    private URLClassLoader testClassLoader;

    public ArcTestContainer(Class<?>... beanClasses) {
        this(Collections.emptyList(), Arrays.asList(beanClasses), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), false, false, Collections.emptyList());
    }

    public ArcTestContainer(List<Class<?>> resourceReferenceProviders, List<Class<?>> beanClasses, List<Class<? extends Annotation>> resourceAnnotations,
            List<BeanRegistrar> beanRegistrars, List<AnnotationsTransformer> annotationsTransformers, List<DeploymentEnhancer> deploymentEnhancers,
            List<BeanDeploymentValidator> beanDeploymentValidators, boolean shouldFail, boolean removeUnusedBeans, List<Predicate<BeanInfo>> exclusions) {
        this.resourceReferenceProviders = resourceReferenceProviders;
        this.beanClasses = beanClasses;
        this.resourceAnnotations = resourceAnnotations;
        this.beanRegistrars = beanRegistrars;
        this.annotationsTransformers = annotationsTransformers;
        this.deploymentEnhancers = deploymentEnhancers;
        this.beanDeploymentValidators = beanDeploymentValidators;
        this.buildFailure = new AtomicReference<Throwable>(null);
        this.shouldFail = shouldFail;
        this.removeUnusedBeans = removeUnusedBeans;
        this.exclusions = exclusions;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ClassLoader oldTccl = init(description.getTestClass());
                try {
                    base.evaluate();
                } finally {
                    Thread.currentThread().setContextClassLoader(oldTccl);
                    if(testClassLoader != null) {
                        try {
                            testClassLoader.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                    shutdown();
                }
            }
        };
    }

    public Throwable getFailure() {
        return buildFailure.get();
    }

    private void shutdown() {
        Arc.shutdown();
    }

    private ClassLoader init(Class<?> testClass) {

        // Make sure Arc is down
        Arc.shutdown();

        // Build index
        Index index;
        try {
            index = index(beanClasses);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create index", e);
        }

        ClassLoader old = Thread.currentThread()
                .getContextClassLoader();

        try {
            String arcContainerAbsolutePath =
                    ArcTestContainer.class.getClassLoader().getResource(ArcTestContainer.class.getName().replace(".", "/") + ".class").getFile();
            int targetClassesIndex = arcContainerAbsolutePath.indexOf(TARGET_TEST_CLASSES);
            String testClassesRootPath = arcContainerAbsolutePath.substring(0, targetClassesIndex);
            File generatedSourcesDirectory = new File("target/generated-arc-sources");
            File testOutputDirectory = new File(testClassesRootPath + TARGET_TEST_CLASSES);
            File componentsProviderFile = new File(generatedSourcesDirectory + "/" + nameToPath(testClass.getPackage()
                    .getName()), ComponentsProvider.class.getSimpleName());

            File resourceReferenceProviderFile = new File(generatedSourcesDirectory + "/" + nameToPath(testClass.getPackage()
                    .getName()), ResourceReferenceProvider.class.getSimpleName());

            if (!resourceReferenceProviders.isEmpty()) {
                try {
                    resourceReferenceProviderFile.getParentFile()
                            .mkdirs();
                    Files.write(resourceReferenceProviderFile.toPath(), resourceReferenceProviders.stream()
                            .map(c -> c.getName())
                            .collect(Collectors.toList()));
                } catch (IOException e) {
                    throw new IllegalStateException("Error generating resource reference providers", e);
                }
            }

            BeanProcessor.Builder beanProcessorBuilder = BeanProcessor.builder()
                    .setName(testClass.getSimpleName())
                    .setIndex(index);
            if (!resourceAnnotations.isEmpty()) {
                beanProcessorBuilder.addResourceAnnotations(resourceAnnotations.stream()
                        .map(c -> DotName.createSimple(c.getName()))
                        .collect(Collectors.toList()));
            }
            for (BeanRegistrar registrar : beanRegistrars) {
                beanProcessorBuilder.addBeanRegistrar(registrar);
            }
            for (AnnotationsTransformer annotationsTransformer : annotationsTransformers) {
                beanProcessorBuilder.addAnnotationTransformer(annotationsTransformer);
            }
            for (DeploymentEnhancer enhancer : deploymentEnhancers) {
                beanProcessorBuilder.addDeploymentEnhancer(enhancer);
            }
            for (BeanDeploymentValidator validator : beanDeploymentValidators) {
                beanProcessorBuilder.addBeanDeploymentValidator(validator);
            }
            beanProcessorBuilder.setOutput(new ResourceOutput() {

                @Override
                public void writeResource(Resource resource) throws IOException {
                    switch (resource.getType()) {
                        case JAVA_CLASS:
                            resource.writeTo(testOutputDirectory);
                            break;
                        case SERVICE_PROVIDER:
                            if (resource.getName()
                                    .endsWith(ComponentsProvider.class.getName())) {
                                componentsProviderFile.getParentFile()
                                        .mkdirs();
                                try (FileOutputStream out = new FileOutputStream(componentsProviderFile)) {
                                    out.write(resource.getData());
                                }
                            }
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                }
            });
            beanProcessorBuilder.setRemoveUnusedBeans(removeUnusedBeans);
            for (Predicate<BeanInfo> exclusion : exclusions) {
                beanProcessorBuilder.addRemovalExclusion(exclusion);
            }

            BeanProcessor beanProcessor = beanProcessorBuilder.build();

            try {
                beanProcessor.process();
            } catch (IOException e) {
                throw new IllegalStateException("Error generating resources", e);
            }

            testClassLoader = new URLClassLoader(new URL[] {}, old) {
                @Override
                public Enumeration<URL> getResources(String name) throws IOException {
                    if (("META-INF/services/" + ComponentsProvider.class.getName()).equals(name)) {
                        // return URL that points to the correct test bean provider
                        return Collections.enumeration(Collections.singleton(componentsProviderFile.toURI()
                                .toURL()));
                    } else if (("META-INF/services/" + ResourceReferenceProvider.class.getName()).equals(name) && !resourceReferenceProviders.isEmpty()) {
                        return Collections.enumeration(Collections.singleton(resourceReferenceProviderFile.toURI()
                                .toURL()));
                    }
                    return super.getResources(name);
                }
            };
            Thread.currentThread()
                    .setContextClassLoader(testClassLoader);

            // Now we are ready to initialize Arc
            Arc.initialize();

        } catch (Throwable e) {
            if (shouldFail) {
                buildFailure.set(e);
            } else {
                throw e;
            }
        }
        return old;
    }

    private Index index(Iterable<Class<?>> classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (InputStream stream = ArcTestContainer.class.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }

    private String nameToPath(String packName) {
        return packName.replace('.', '/');
    }
}

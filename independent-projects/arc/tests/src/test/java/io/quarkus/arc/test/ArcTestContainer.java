package io.quarkus.arc.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;

import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcInitConfig;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.ResourceReferenceProvider;
import io.quarkus.arc.processor.AlternativePriorities;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanArchives;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.arc.processor.ObserverTransformer;
import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.arc.processor.ResourceOutput;
import io.quarkus.arc.processor.StereotypeRegistrar;
import io.quarkus.arc.processor.bcextensions.ExtensionsEntryPoint;

/**
 * Junit5 extension for Arc bootstrap/shutdown.
 * Designed to be used via {code @RegisterExtension} fields in tests.
 *
 * It bootstraps Arc before each test method and shuts down afterwards.
 * Leverages root {@code ExtensionContext.Store} to store and retrieve some variables.
 */
public class ArcTestContainer implements BeforeEachCallback, AfterEachCallback {

    // our specific namespace for storing anything into ExtensionContext.Store
    private static ExtensionContext.Namespace EXTENSION_NAMESPACE;

    // Strings used as keys in ExtensionContext.Store
    private static final String KEY_OLD_TCCL = "arcExtensionOldTccl";

    private static final String TARGET_TEST_CLASSES = "target/test-classes";

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final List<Class<?>> resourceReferenceProviders;
        private final List<Class<?>> beanClasses;
        private final List<Class<?>> additionalClasses;
        private final List<Class<? extends Annotation>> resourceAnnotations;
        private final List<BeanRegistrar> beanRegistrars;
        private final List<ObserverRegistrar> observerRegistrars;
        private final List<ContextRegistrar> contextRegistrars;
        private final List<QualifierRegistrar> qualifierRegistrars;
        private final List<InterceptorBindingRegistrar> interceptorBindingRegistrars;
        private final List<StereotypeRegistrar> stereotypeRegistrars;
        private final List<AnnotationTransformation> annotationsTransformers;
        private final List<InjectionPointsTransformer> injectionsPointsTransformers;
        private final List<ObserverTransformer> observerTransformers;
        private final List<BeanDeploymentValidator> beanDeploymentValidators;
        private boolean shouldFail = false;
        private boolean removeUnusedBeans = false;
        private final List<Predicate<BeanInfo>> removalExclusions;
        private AlternativePriorities alternativePriorities;
        private final List<BuildCompatibleExtension> buildCompatibleExtensions;
        private boolean strictCompatibility = false;
        private boolean optimizeContexts = false;
        private final List<Predicate<ClassInfo>> excludeTypes;

        public Builder() {
            resourceReferenceProviders = new ArrayList<>();
            beanClasses = new ArrayList<>();
            additionalClasses = new ArrayList<>();
            resourceAnnotations = new ArrayList<>();
            beanRegistrars = new ArrayList<>();
            observerRegistrars = new ArrayList<>();
            contextRegistrars = new ArrayList<>();
            qualifierRegistrars = new ArrayList<>();
            interceptorBindingRegistrars = new ArrayList<>();
            stereotypeRegistrars = new ArrayList<>();
            annotationsTransformers = new ArrayList<>();
            injectionsPointsTransformers = new ArrayList<>();
            observerTransformers = new ArrayList<>();
            beanDeploymentValidators = new ArrayList<>();
            removalExclusions = new ArrayList<>();
            buildCompatibleExtensions = new ArrayList<>();
            excludeTypes = new ArrayList<>();
        }

        public Builder resourceReferenceProviders(Class<?>... resourceReferenceProviders) {
            Collections.addAll(this.resourceReferenceProviders, resourceReferenceProviders);
            return this;
        }

        public Builder beanClasses(Class<?>... beanClasses) {
            Collections.addAll(this.beanClasses, beanClasses);
            return this;
        }

        public Builder additionalClasses(Class<?>... additionalClasses) {
            Collections.addAll(this.additionalClasses, additionalClasses);
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

        public Builder observerRegistrars(ObserverRegistrar... registrars) {
            Collections.addAll(this.observerRegistrars, registrars);
            return this;
        }

        public Builder contextRegistrars(ContextRegistrar... registrars) {
            Collections.addAll(this.contextRegistrars, registrars);
            return this;
        }

        /**
         * @deprecated use {@link #annotationTransformations(AnnotationTransformation...)}
         */
        @Deprecated(forRemoval = true)
        public Builder annotationsTransformers(AnnotationsTransformer... transformers) {
            Collections.addAll(this.annotationsTransformers, transformers);
            return this;
        }

        public Builder annotationTransformations(AnnotationTransformation... transformations) {
            Collections.addAll(this.annotationsTransformers, transformations);
            return this;
        }

        public Builder injectionPointsTransformers(InjectionPointsTransformer... transformers) {
            Collections.addAll(this.injectionsPointsTransformers, transformers);
            return this;
        }

        public Builder observerTransformers(ObserverTransformer... transformers) {
            Collections.addAll(this.observerTransformers, transformers);
            return this;
        }

        public Builder qualifierRegistrars(QualifierRegistrar... registrars) {
            Collections.addAll(this.qualifierRegistrars, registrars);
            return this;
        }

        public Builder interceptorBindingRegistrars(InterceptorBindingRegistrar... registrars) {
            Collections.addAll(this.interceptorBindingRegistrars, registrars);
            return this;
        }

        public Builder stereotypeRegistrars(StereotypeRegistrar... registrars) {
            Collections.addAll(this.stereotypeRegistrars, registrars);
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
            this.removalExclusions.add(exclusion);
            return this;
        }

        public Builder shouldFail() {
            this.shouldFail = true;
            return this;
        }

        public Builder alternativePriorities(AlternativePriorities priorities) {
            this.alternativePriorities = priorities;
            return this;
        }

        public final Builder buildCompatibleExtensions(BuildCompatibleExtension... extensions) {
            Collections.addAll(this.buildCompatibleExtensions, extensions);
            return this;
        }

        public Builder strictCompatibility(boolean strictCompatibility) {
            this.strictCompatibility = strictCompatibility;
            return this;
        }

        public Builder optimizeContexts(boolean value) {
            this.optimizeContexts = value;
            return this;
        }

        public Builder excludeType(Predicate<ClassInfo> predicate) {
            this.excludeTypes.add(predicate);
            return this;
        }

        public ArcTestContainer build() {
            return new ArcTestContainer(this);
        }

    }

    private final List<Class<?>> resourceReferenceProviders;

    private final List<Class<?>> beanClasses;
    private final List<Class<?>> additionalClasses;
    private final List<Predicate<ClassInfo>> excludeTypes;

    private final List<Class<? extends Annotation>> resourceAnnotations;

    private final List<BeanRegistrar> beanRegistrars;
    private final List<ObserverRegistrar> observerRegistrars;
    private final List<ContextRegistrar> contextRegistrars;
    private final List<QualifierRegistrar> qualifierRegistrars;
    private final List<InterceptorBindingRegistrar> interceptorBindingRegistrars;
    private final List<StereotypeRegistrar> stereotypeRegistrars;
    private final List<AnnotationTransformation> annotationsTransformers;
    private final List<InjectionPointsTransformer> injectionPointsTransformers;
    private final List<ObserverTransformer> observerTransformers;
    private final List<BeanDeploymentValidator> beanDeploymentValidators;

    private final boolean shouldFail;
    private final AtomicReference<Throwable> buildFailure;

    private final boolean removeUnusedBeans;
    private final List<Predicate<BeanInfo>> removalExclusions;

    private final AlternativePriorities alternativePriorities;

    private final List<BuildCompatibleExtension> buildCompatibleExtensions;

    private final boolean strictCompatibility;
    private final boolean optimizeContexts;

    public ArcTestContainer(Class<?>... beanClasses) {
        this.resourceReferenceProviders = Collections.emptyList();
        this.beanClasses = Arrays.asList(beanClasses);
        this.additionalClasses = Collections.emptyList();
        this.resourceAnnotations = Collections.emptyList();
        this.beanRegistrars = Collections.emptyList();
        this.observerRegistrars = Collections.emptyList();
        this.contextRegistrars = Collections.emptyList();
        this.interceptorBindingRegistrars = Collections.emptyList();
        this.stereotypeRegistrars = Collections.emptyList();
        this.qualifierRegistrars = Collections.emptyList();
        this.annotationsTransformers = Collections.emptyList();
        this.injectionPointsTransformers = Collections.emptyList();
        this.observerTransformers = Collections.emptyList();
        this.beanDeploymentValidators = Collections.emptyList();
        this.buildFailure = new AtomicReference<Throwable>(null);
        this.shouldFail = false;
        this.removeUnusedBeans = false;
        this.removalExclusions = Collections.emptyList();
        this.alternativePriorities = null;
        this.buildCompatibleExtensions = Collections.emptyList();
        this.strictCompatibility = false;
        this.optimizeContexts = false;
        this.excludeTypes = Collections.emptyList();
    }

    public ArcTestContainer(Builder builder) {
        this.resourceReferenceProviders = builder.resourceReferenceProviders;
        this.beanClasses = builder.beanClasses;
        this.additionalClasses = builder.additionalClasses;
        this.resourceAnnotations = builder.resourceAnnotations;
        this.beanRegistrars = builder.beanRegistrars;
        this.observerRegistrars = builder.observerRegistrars;
        this.contextRegistrars = builder.contextRegistrars;
        this.qualifierRegistrars = builder.qualifierRegistrars;
        this.interceptorBindingRegistrars = builder.interceptorBindingRegistrars;
        this.stereotypeRegistrars = builder.stereotypeRegistrars;
        this.annotationsTransformers = builder.annotationsTransformers;
        this.injectionPointsTransformers = builder.injectionsPointsTransformers;
        this.observerTransformers = builder.observerTransformers;
        this.beanDeploymentValidators = builder.beanDeploymentValidators;
        this.buildFailure = new AtomicReference<Throwable>(null);
        this.shouldFail = builder.shouldFail;
        this.removeUnusedBeans = builder.removeUnusedBeans;
        this.removalExclusions = builder.removalExclusions;
        this.alternativePriorities = builder.alternativePriorities;
        this.buildCompatibleExtensions = builder.buildCompatibleExtensions;
        this.strictCompatibility = builder.strictCompatibility;
        this.optimizeContexts = builder.optimizeContexts;
        this.excludeTypes = builder.excludeTypes;
    }

    // this is where we start Arc, we operate on a per-method basis
    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        getRootExtensionStore(extensionContext).put(KEY_OLD_TCCL, init(extensionContext));
    }

    // this is where we shutdown Arc
    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        ClassLoader oldTccl = getRootExtensionStore(extensionContext).get(KEY_OLD_TCCL, ClassLoader.class);
        Thread.currentThread().setContextClassLoader(oldTccl);
        shutdown();
    }

    private static synchronized ExtensionContext.Store getRootExtensionStore(ExtensionContext context) {
        if (EXTENSION_NAMESPACE == null) {
            EXTENSION_NAMESPACE = ExtensionContext.Namespace.create(ArcTestContainer.class);
        }
        return context.getRoot().getStore(EXTENSION_NAMESPACE);
    }

    /**
     * In case the test is expected to fail, this method will return a {@link Throwable} that caused it.
     */
    public Throwable getFailure() {
        return buildFailure.get();
    }

    private void shutdown() {
        Arc.shutdown();
    }

    private ClassLoader init(ExtensionContext context) {
        // retrieve test class from extension context
        Class<?> testClass = context.getRequiredTestClass();

        // Make sure Arc is down
        Arc.shutdown();

        // Build index
        IndexView immutableBeanArchiveIndex;
        try {
            immutableBeanArchiveIndex = BeanArchives.buildImmutableBeanArchiveIndex(index(beanClasses));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create index", e);
        }

        IndexView applicationIndex;
        if (additionalClasses.isEmpty()) {
            applicationIndex = null;
        } else {
            try {
                applicationIndex = index(additionalClasses);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create index", e);
            }
        }

        ExtensionsEntryPoint buildCompatibleExtensions = new ExtensionsEntryPoint(this.buildCompatibleExtensions);

        {
            IndexView overallIndex = applicationIndex != null
                    ? CompositeIndex.create(immutableBeanArchiveIndex, applicationIndex)
                    : immutableBeanArchiveIndex;
            Set<String> additionalClasses = new HashSet<>();
            buildCompatibleExtensions.runDiscovery(overallIndex, additionalClasses);
            Index additionalIndex = null;
            try {
                Set<Class<?>> additionalClassObjects = new HashSet<>();
                for (String additionalClass : additionalClasses) {
                    additionalClassObjects.add(ArcTestContainer.class.getClassLoader().loadClass(additionalClass));
                }
                additionalIndex = index(additionalClassObjects);
            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException("Failed to create index", e);
            }
            immutableBeanArchiveIndex = CompositeIndex.create(immutableBeanArchiveIndex, additionalIndex);
        }

        ClassLoader old = Thread.currentThread().getContextClassLoader();

        try {
            String arcContainerAbsolutePath = ArcTestContainer.class.getClassLoader()
                    .getResource(ArcTestContainer.class.getName().replace(".", "/") + ".class").getFile();
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

            String deploymentName = testClass.getName().replace('.', '_');
            BeanProcessor.Builder builder = BeanProcessor.builder()
                    .setName(deploymentName)
                    .setImmutableBeanArchiveIndex(immutableBeanArchiveIndex)
                    .setComputingBeanArchiveIndex(BeanArchives.buildComputingBeanArchiveIndex(getClass().getClassLoader(),
                            new ConcurrentHashMap<>(), immutableBeanArchiveIndex))
                    .setApplicationIndex(applicationIndex)
                    .setBuildCompatibleExtensions(buildCompatibleExtensions)
                    .setStrictCompatibility(strictCompatibility)
                    .setOptimizeContexts(optimizeContexts);
            if (!resourceAnnotations.isEmpty()) {
                builder.addResourceAnnotations(resourceAnnotations.stream()
                        .map(c -> DotName.createSimple(c.getName()))
                        .collect(Collectors.toList()));
            }
            beanRegistrars.forEach(builder::addBeanRegistrar);
            observerRegistrars.forEach(builder::addObserverRegistrar);
            contextRegistrars.forEach(builder::addContextRegistrar);
            qualifierRegistrars.forEach(builder::addQualifierRegistrar);
            interceptorBindingRegistrars.forEach(builder::addInterceptorBindingRegistrar);
            stereotypeRegistrars.forEach(builder::addStereotypeRegistrar);
            annotationsTransformers.forEach(builder::addAnnotationTransformation);
            injectionPointsTransformers.forEach(builder::addInjectionPointTransformer);
            observerTransformers.forEach(builder::addObserverTransformer);
            beanDeploymentValidators.forEach(builder::addBeanDeploymentValidator);
            excludeTypes.forEach(builder::addExcludeType);
            builder.setOutput(new ResourceOutput() {

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
            builder.setRemoveUnusedBeans(removeUnusedBeans);
            for (Predicate<BeanInfo> exclusion : removalExclusions) {
                builder.addRemovalExclusion(exclusion);
            }
            builder.setAlternativePriorities(alternativePriorities);

            BeanProcessor beanProcessor = builder.build();

            try {
                beanProcessor.process();
            } catch (IOException e) {
                throw new IllegalStateException("Error generating resources", e);
            }

            ArcTestClassLoader testClassLoader = new ArcTestClassLoader(old, componentsProviderFile,
                    resourceReferenceProviders.isEmpty() ? null : resourceReferenceProviderFile);
            Thread.currentThread()
                    .setContextClassLoader(testClassLoader);

            // Now we are ready to initialize Arc
            ArcInitConfig.Builder initConfigBuilder = ArcInitConfig.builder();
            initConfigBuilder.setStrictCompatibility(strictCompatibility);
            Arc.initialize(initConfigBuilder.build());

        } catch (Throwable e) {
            if (shouldFail) {
                buildFailure.set(e);
            } else {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            if (shouldFail && buildFailure.get() == null) {
                throw new AssertionError("The container was expected to fail!");
            }
        }
        return old;
    }

    private Index index(Iterable<Class<?>> classes) throws IOException {
        Indexer indexer = new Indexer();
        Set<String> packages = new HashSet<>();
        for (Class<?> clazz : classes) {
            packages.add(clazz.getPackageName());
            try (InputStream stream = ArcTestContainer.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        for (String pkg : packages) {
            try (InputStream stream = ArcTestContainer.class.getClassLoader()
                    .getResourceAsStream(pkg.replace('.', '/') + "/package-info.class")) {
                if (stream != null) {
                    indexer.index(stream);
                }
            }
        }
        return indexer.complete();
    }

    private String nameToPath(String packName) {
        return packName.replace('.', '/');
    }

}

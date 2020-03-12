package io.quarkus.arc.test;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.ResourceReferenceProvider;
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
import io.quarkus.arc.processor.ResourceOutput;
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
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

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
    private static final String KEY_TEST_CLASSLOADER = "arcExtensionTestClassLoader";

    private static final String TARGET_TEST_CLASSES = "target/test-classes";

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final List<Class<?>> resourceReferenceProviders;
        private final List<Class<?>> beanClasses;
        private final List<Class<? extends Annotation>> resourceAnnotations;
        private final List<BeanRegistrar> beanRegistrars;
        private final List<ObserverRegistrar> observerRegistrars;
        private final List<ContextRegistrar> contextRegistrars;
        private final List<InterceptorBindingRegistrar> interceptorBindingRegistrars;
        private final List<AnnotationsTransformer> annotationsTransformers;
        private final List<InjectionPointsTransformer> injectionsPointsTransformers;
        private final List<ObserverTransformer> observerTransformers;
        private final List<BeanDeploymentValidator> beanDeploymentValidators;
        private boolean shouldFail = false;
        private boolean removeUnusedBeans = false;
        private final List<Predicate<BeanInfo>> exclusions;

        public Builder() {
            resourceReferenceProviders = new ArrayList<>();
            beanClasses = new ArrayList<>();
            resourceAnnotations = new ArrayList<>();
            beanRegistrars = new ArrayList<>();
            observerRegistrars = new ArrayList<>();
            contextRegistrars = new ArrayList<>();
            interceptorBindingRegistrars = new ArrayList<>();
            annotationsTransformers = new ArrayList<>();
            injectionsPointsTransformers = new ArrayList<>();
            observerTransformers = new ArrayList<>();
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

        public Builder observerRegistrars(ObserverRegistrar... registrars) {
            Collections.addAll(this.observerRegistrars, registrars);
            return this;
        }

        public Builder contextRegistrars(ContextRegistrar... registrars) {
            Collections.addAll(this.contextRegistrars, registrars);
            return this;
        }

        public Builder annotationsTransformers(AnnotationsTransformer... transformers) {
            Collections.addAll(this.annotationsTransformers, transformers);
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

        public Builder interceptorBindingRegistrars(InterceptorBindingRegistrar... registrars) {
            Collections.addAll(this.interceptorBindingRegistrars, registrars);
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
            return new ArcTestContainer(resourceReferenceProviders, beanClasses, resourceAnnotations, beanRegistrars,
                    observerRegistrars, contextRegistrars, interceptorBindingRegistrars, annotationsTransformers,
                    injectionsPointsTransformers,
                    observerTransformers, beanDeploymentValidators, shouldFail, removeUnusedBeans, exclusions);
        }

    }

    private final List<Class<?>> resourceReferenceProviders;

    private final List<Class<?>> beanClasses;

    private final List<Class<? extends Annotation>> resourceAnnotations;

    private final List<BeanRegistrar> beanRegistrars;

    private final List<ObserverRegistrar> observerRegistrars;

    private final List<ContextRegistrar> contextRegistrars;

    List<InterceptorBindingRegistrar> bindingRegistrars;

    private final List<AnnotationsTransformer> annotationsTransformers;

    private final List<InjectionPointsTransformer> injectionPointsTransformers;

    private final List<ObserverTransformer> observerTransformers;

    private final List<BeanDeploymentValidator> beanDeploymentValidators;

    private final boolean shouldFail;
    private final AtomicReference<Throwable> buildFailure;

    private final boolean removeUnusedBeans;
    private final List<Predicate<BeanInfo>> exclusions;

    public ArcTestContainer(Class<?>... beanClasses) {
        this(Collections.emptyList(), Arrays.asList(beanClasses), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false,
                Collections.emptyList());
    }

    public ArcTestContainer(List<Class<?>> resourceReferenceProviders, List<Class<?>> beanClasses,
            List<Class<? extends Annotation>> resourceAnnotations,
            List<BeanRegistrar> beanRegistrars, List<ObserverRegistrar> observerRegistrars,
            List<ContextRegistrar> contextRegistrars,
            List<InterceptorBindingRegistrar> bindingRegistrars,
            List<AnnotationsTransformer> annotationsTransformers, List<InjectionPointsTransformer> ipTransformers,
            List<ObserverTransformer> observerTransformers,
            List<BeanDeploymentValidator> beanDeploymentValidators, boolean shouldFail, boolean removeUnusedBeans,
            List<Predicate<BeanInfo>> exclusions) {
        this.resourceReferenceProviders = resourceReferenceProviders;
        this.beanClasses = beanClasses;
        this.resourceAnnotations = resourceAnnotations;
        this.beanRegistrars = beanRegistrars;
        this.observerRegistrars = observerRegistrars;
        this.contextRegistrars = contextRegistrars;
        this.bindingRegistrars = bindingRegistrars;
        this.annotationsTransformers = annotationsTransformers;
        this.injectionPointsTransformers = ipTransformers;
        this.observerTransformers = observerTransformers;
        this.beanDeploymentValidators = beanDeploymentValidators;
        this.buildFailure = new AtomicReference<Throwable>(null);
        this.shouldFail = shouldFail;
        this.removeUnusedBeans = removeUnusedBeans;
        this.exclusions = exclusions;
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

        URLClassLoader testClassLoader = getRootExtensionStore(extensionContext).get(KEY_TEST_CLASSLOADER,
                URLClassLoader.class);
        if (testClassLoader != null) {
            try {
                testClassLoader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        Index index;
        try {
            index = index(beanClasses);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create index", e);
        }

        ClassLoader old = Thread.currentThread()
                .getContextClassLoader();

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

            BeanProcessor.Builder builder = BeanProcessor.builder()
                    .setName(testClass.getSimpleName())
                    .setIndex(BeanArchives.buildBeanArchiveIndex(getClass().getClassLoader(), index));
            if (!resourceAnnotations.isEmpty()) {
                builder.addResourceAnnotations(resourceAnnotations.stream()
                        .map(c -> DotName.createSimple(c.getName()))
                        .collect(Collectors.toList()));
            }
            beanRegistrars.forEach(builder::addBeanRegistrar);
            observerRegistrars.forEach(builder::addObserverRegistrar);
            contextRegistrars.forEach(builder::addContextRegistrar);
            bindingRegistrars.forEach(builder::addInterceptorbindingRegistrar);
            annotationsTransformers.forEach(builder::addAnnotationTransformer);
            injectionPointsTransformers.forEach(builder::addInjectionPointTransformer);
            observerTransformers.forEach(builder::addObserverTransformer);
            beanDeploymentValidators.forEach(builder::addBeanDeploymentValidator);
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
            for (Predicate<BeanInfo> exclusion : exclusions) {
                builder.addRemovalExclusion(exclusion);
            }

            BeanProcessor beanProcessor = builder.build();

            try {
                beanProcessor.process();
            } catch (IOException e) {
                throw new IllegalStateException("Error generating resources", e);
            }

            URLClassLoader testClassLoader = new URLClassLoader(new URL[] {}, old) {
                @Override
                public Enumeration<URL> getResources(String name) throws IOException {
                    if (("META-INF/services/" + ComponentsProvider.class.getName()).equals(name)) {
                        // return URL that points to the correct test bean provider
                        return Collections.enumeration(Collections.singleton(componentsProviderFile.toURI()
                                .toURL()));
                    } else if (("META-INF/services/" + ResourceReferenceProvider.class.getName()).equals(name)
                            && !resourceReferenceProviders.isEmpty()) {
                        return Collections.enumeration(Collections.singleton(resourceReferenceProviderFile.toURI()
                                .toURL()));
                    }
                    return super.getResources(name);
                }
            };
            Thread.currentThread()
                    .setContextClassLoader(testClassLoader);

            // store the test class loader into extension store
            getRootExtensionStore(context).put(KEY_TEST_CLASSLOADER, testClassLoader);

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
            try (InputStream stream = ArcTestContainer.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }

    private String nameToPath(String packName) {
        return packName.replace('.', '/');
    }
}

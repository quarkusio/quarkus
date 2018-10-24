package org.jboss.protean.arc.processor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
import org.jboss.protean.arc.ActivateRequestContextInterceptor;
import org.jboss.protean.arc.processor.BuildProcessor.BuildContext;
import org.jboss.protean.arc.processor.DeploymentEnhancer.DeploymentContext;
import org.jboss.protean.arc.processor.ResourceOutput.Resource;
import org.jboss.protean.arc.processor.ResourceOutput.Resource.SpecialType;

/**
 *
 * @author Martin Kouba
 */
public class BeanProcessor {

    public static Builder builder() {
        return new Builder();
    }

    static final String DEFAULT_NAME = "Default";

    private static final Logger LOGGER = Logger.getLogger(BeanProcessor.class);

    private final String name;

    private final IndexView index;

    private final Collection<DotName> additionalBeanDefiningAnnotations;

    private final ResourceOutput output;

    private final boolean sharedAnnotationLiterals;

    private final ReflectionRegistration reflectionRegistration;

    private final Collection<DotName> resourceAnnotations;

    private final List<AnnotationsTransformer> annotationTransformers;

    private final List<BeanRegistrar> beanRegistrars;

    private BeanProcessor(String name, IndexView index, Collection<DotName> additionalBeanDefiningAnnotations, ResourceOutput output,
            boolean sharedAnnotationLiterals, ReflectionRegistration reflectionRegistration, List<AnnotationsTransformer> annotationTransformers,
            Collection<DotName> resourceAnnotations, List<BeanRegistrar> beanRegistrars, List<DeploymentEnhancer> deploymentEnhancers) {
        this.reflectionRegistration = reflectionRegistration;
        Objects.requireNonNull(output);
        this.name = name;
        this.additionalBeanDefiningAnnotations = additionalBeanDefiningAnnotations;
        this.output = output;
        this.sharedAnnotationLiterals = sharedAnnotationLiterals;
        this.annotationTransformers = annotationTransformers;
        this.resourceAnnotations = resourceAnnotations;

        // Initialize all build processors
        ConcurrentMap<String, Object> data = new ConcurrentHashMap<>();
        BuildContext buildContext = new BuildContext() {
            @Override
            public IndexView getIndex() {
                return index;
            }

            @Override
            public Map<String, Object> getContextData() {
                return data;
            }
        };
        for (AnnotationsTransformer transformer : annotationTransformers) {
            transformer.initialize(buildContext);
        }
        for (DeploymentEnhancer enhancer : deploymentEnhancers) {
            enhancer.initialize(buildContext);
        }
        for (BeanRegistrar registrar : beanRegistrars) {
            registrar.initialize(buildContext);
        }

        if (!deploymentEnhancers.isEmpty()) {
            Indexer indexer = new Indexer();
            DeploymentContext deploymentContext = new DeploymentContext() {

                @Override
                public void addClass(String className) {
                    index(indexer, className);
                }

                @Override
                public void addClass(Class<?> clazz) {
                    index(indexer, clazz.getName());
                }
            };
            deploymentEnhancers.sort(BuildProcessor::compare);
            for (DeploymentEnhancer enhancer : deploymentEnhancers) {
                enhancer.enhance(deploymentContext);
            }
            this.index = CompositeIndex.create(index, indexer.complete());
        } else {
            this.index = index;
        }

        beanRegistrars.sort(BuildProcessor::compare);
        this.beanRegistrars = beanRegistrars;
    }

    public BeanDeployment process() throws IOException {

        BeanDeployment beanDeployment = new BeanDeployment(new IndexWrapper(index), additionalBeanDefiningAnnotations, annotationTransformers,
                resourceAnnotations, beanRegistrars);
        beanDeployment.init();

        AnnotationLiteralProcessor annotationLiterals = new AnnotationLiteralProcessor(name, sharedAnnotationLiterals);
        BeanGenerator beanGenerator = new BeanGenerator(annotationLiterals);
        ClientProxyGenerator clientProxyGenerator = new ClientProxyGenerator();
        InterceptorGenerator interceptorGenerator = new InterceptorGenerator(annotationLiterals);
        SubclassGenerator subclassGenerator = new SubclassGenerator(annotationLiterals);
        ObserverGenerator observerGenerator = new ObserverGenerator(annotationLiterals);
        AnnotationLiteralGenerator annotationLiteralsGenerator = new AnnotationLiteralGenerator();

        Map<BeanInfo, String> beanToGeneratedName = new HashMap<>();
        Map<ObserverInfo, String> observerToGeneratedName = new HashMap<>();
        Map<InterceptorInfo, String> interceptorToGeneratedName = new HashMap<>();

        long start = System.currentTimeMillis();
        List<Resource> resources = new ArrayList<>();

        // Generate interceptors
        for (InterceptorInfo interceptor : beanDeployment.getInterceptors()) {
            for (Resource resource : interceptorGenerator.generate(interceptor, reflectionRegistration)) {
                resources.add(resource);
                if (SpecialType.INTERCEPTOR_BEAN.equals(resource.getSpecialType())) {
                    interceptorToGeneratedName.put(interceptor, resource.getName());
                    beanToGeneratedName.put(interceptor, resource.getName());
                }
            }
        }

        // Generate beans
        for (BeanInfo bean : beanDeployment.getBeans()) {
            for (Resource resource : beanGenerator.generate(bean, reflectionRegistration)) {
                resources.add(resource);
                if (SpecialType.BEAN.equals(resource.getSpecialType())) {
                    if (bean.getScope().isNormal()) {
                        // Generate client proxy
                        resources.addAll(clientProxyGenerator.generate(bean, resource.getFullyQualifiedName(), reflectionRegistration));
                    }
                    beanToGeneratedName.put(bean, resource.getName());
                    if (bean.isSubclassRequired()) {
                        resources.addAll(subclassGenerator.generate(bean, resource.getFullyQualifiedName(), reflectionRegistration));
                    }
                }
            }
        }

        // Generate observers
        for (ObserverInfo observer : beanDeployment.getObservers()) {
            for (Resource resource : observerGenerator.generate(observer, reflectionRegistration)) {
                resources.add(resource);
                if (SpecialType.OBSERVER.equals(resource.getSpecialType())) {
                    observerToGeneratedName.put(observer, resource.getName());
                }
            }
        }

        // Generate _ComponentsProvider
        resources.addAll(new ComponentsProviderGenerator().generate(name, beanDeployment, beanToGeneratedName, observerToGeneratedName));

        // Generate AnnotationLiterals
        if (annotationLiterals.hasLiteralsToGenerate()) {
            resources.addAll(annotationLiteralsGenerator.generate(name, beanDeployment, annotationLiterals.getCache()));
        }

        for (Resource resource : resources) {
            output.writeResource(resource);
        }
        LOGGER.infof("%s resources generated/written in %s ms", resources.size(), System.currentTimeMillis() - start);
        return beanDeployment;
    }

    private static IndexView addBuiltinClasses(IndexView index) {
        Indexer indexer = new Indexer();
        // Add builtin interceptors and bindings
        index(indexer, ActivateRequestContext.class.getName());
        index(indexer, ActivateRequestContextInterceptor.class.getName());
        // Add builtin qualifiers if needed
        if (index.getClassByName(DotNames.ANY) == null) {
            index(indexer, Default.class.getName());
            index(indexer, Any.class.getName());
            index(indexer, Named.class.getName());
        }
        return CompositeIndex.create(index, indexer.complete());
    }

    private static void index(Indexer indexer, String className) {
        try (InputStream stream = BeanProcessor.class.getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class")) {
            indexer.index(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to index: " + className);
        }
    }

    public static class Builder {

        private String name = DEFAULT_NAME;

        private IndexView index;

        private Collection<DotName> additionalBeanDefiningAnnotations = Collections.emptySet();

        private ResourceOutput output;

        private boolean sharedAnnotationLiterals = true;

        private ReflectionRegistration reflectionRegistration = ReflectionRegistration.NOOP;

        private final List<DotName> resourceAnnotations = new ArrayList<>();

        private final List<AnnotationsTransformer> annotationTransformers = new ArrayList<>();
        private final List<BeanRegistrar> beanRegistrars = new ArrayList<>();
        private final List<DeploymentEnhancer> deploymentEnhancers = new ArrayList<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setIndex(IndexView index) {
            this.index = index;
            return this;
        }

        public Builder setAdditionalBeanDefiningAnnotations(Collection<DotName> additionalBeanDefiningAnnotations) {
            this.additionalBeanDefiningAnnotations = additionalBeanDefiningAnnotations;
            return this;
        }

        public Builder setOutput(ResourceOutput output) {
            this.output = output;
            return this;
        }

        public Builder setSharedAnnotationLiterals(boolean sharedAnnotationLiterals) {
            this.sharedAnnotationLiterals = sharedAnnotationLiterals;
            return this;
        }

        public Builder setReflectionRegistration(ReflectionRegistration reflectionRegistration) {
            this.reflectionRegistration = reflectionRegistration;
            return this;
        }

        public Builder addAnnotationTransformer(AnnotationsTransformer transformer) {
            this.annotationTransformers.add(transformer);
            return this;
        }

        public Builder addResourceAnnotations(Collection<DotName> resourceAnnotations) {
            this.resourceAnnotations.addAll(resourceAnnotations);
            return this;
        }

        public Builder addBeanRegistrar(BeanRegistrar registrar) {
            this.beanRegistrars.add(registrar);
            return this;
        }

        public Builder addDeploymentEnhancer(DeploymentEnhancer enhancer) {
            this.deploymentEnhancers.add(enhancer);
            return this;
        }

        public BeanProcessor build() {
            return new BeanProcessor(name, addBuiltinClasses(index), additionalBeanDefiningAnnotations, output, sharedAnnotationLiterals,
                    reflectionRegistration, annotationTransformers, resourceAnnotations, beanRegistrars, deploymentEnhancers);
        }

    }

    /**
     * This wrapper is used to index JDK classes on demand.
     */
    public static class IndexWrapper implements IndexView {

        private final Map<DotName, ClassInfo> additionalClasses;

        private final IndexView index;

        public IndexWrapper(IndexView index) {
            this.index = index;
            this.additionalClasses = new ConcurrentHashMap<>();
        }

        @Override
        public Collection<ClassInfo> getKnownClasses() {
            return index.getKnownClasses();
        }

        @Override
        public ClassInfo getClassByName(DotName className) {
            ClassInfo classInfo = index.getClassByName(className);
            if (classInfo == null) {
                return additionalClasses.computeIfAbsent(className, name -> {
                    LOGGER.debugf("Index: %s", className);
                    Indexer indexer = new Indexer();
                    index(indexer, className.toString());
                    Index index = indexer.complete();
                    return index.getClassByName(name);
                });
            }
            return classInfo;
        }

        @Override
        public Collection<ClassInfo> getKnownDirectSubclasses(DotName className) {
            return index.getKnownDirectSubclasses(className);
        }

        @Override
        public Collection<ClassInfo> getAllKnownSubclasses(DotName className) {
            return index.getAllKnownSubclasses(className);
        }

        @Override
        public Collection<ClassInfo> getKnownDirectImplementors(DotName className) {
            return index.getKnownDirectImplementors(className);
        }

        @Override
        public Collection<ClassInfo> getAllKnownImplementors(DotName interfaceName) {
            return index.getAllKnownImplementors(interfaceName);
        }

        @Override
        public Collection<AnnotationInstance> getAnnotations(DotName annotationName) {
            return index.getAnnotations(annotationName);
        }

    }

}

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

    private BeanProcessor(String name, IndexView index, Collection<DotName> additionalBeanDefiningAnnotations, ResourceOutput output,
            boolean sharedAnnotationLiterals, ReflectionRegistration reflectionRegistration) {
        this.reflectionRegistration = reflectionRegistration;
        Objects.requireNonNull(output);
        this.name = name;
        this.index = index;
        this.additionalBeanDefiningAnnotations = additionalBeanDefiningAnnotations;
        this.output = output;
        this.sharedAnnotationLiterals = sharedAnnotationLiterals;
    }

    public BeanDeployment process() throws IOException {

        BeanDeployment beanDeployment = new BeanDeployment(new IndexWrapper(index), additionalBeanDefiningAnnotations);
        beanDeployment.init();

        BeanGenerator beanGenerator = new BeanGenerator();
        ClientProxyGenerator clientProxyGenerator = new ClientProxyGenerator();
        InterceptorGenerator interceptorGenerator = new InterceptorGenerator();
        SubclassGenerator subclassGenerator = new SubclassGenerator();
        ObserverGenerator observerGenerator = new ObserverGenerator();
        AnnotationLiteralGenerator annotationLiteralsGenerator = new AnnotationLiteralGenerator();

        Map<BeanInfo, String> beanToGeneratedName = new HashMap<>();
        Map<ObserverInfo, String> observerToGeneratedName = new HashMap<>();
        Map<InterceptorInfo, String> interceptorToGeneratedName = new HashMap<>();

        AnnotationLiteralProcessor annotationLiterals = new AnnotationLiteralProcessor(name, sharedAnnotationLiterals);

        long start = System.currentTimeMillis();
        List<Resource> resources = new ArrayList<>();

        // Generate interceptors
        for (InterceptorInfo interceptor : beanDeployment.getInterceptors()) {
            for (Resource resource : interceptorGenerator.generate(interceptor, annotationLiterals, reflectionRegistration)) {
                resources.add(resource);
                if (SpecialType.INTERCEPTOR_BEAN.equals(resource.getSpecialType())) {
                    interceptorToGeneratedName.put(interceptor, resource.getName());
                    beanToGeneratedName.put(interceptor, resource.getName());
                }
            }
        }

        // Generate beans
        for (BeanInfo bean : beanDeployment.getBeans()) {
            for (Resource resource : beanGenerator.generate(bean, annotationLiterals, reflectionRegistration)) {
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
            for (Resource resource : observerGenerator.generate(observer, annotationLiterals, reflectionRegistration)) {
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
        LOGGER.infof("%s resources written in %s ms", resources.size(), System.currentTimeMillis() - start);
        return beanDeployment;
    }

    private static IndexView addBuiltinQualifiersIfNeeded(IndexView index) {
        if (index.getClassByName(DotNames.ANY) == null) {
            Indexer indexer = new Indexer();
            index(indexer, Default.class.getName());
            index(indexer, Any.class.getName());
            index(indexer, Named.class.getName());
            return CompositeIndex.create(index, indexer.complete());
        }
        return index;
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

        public BeanProcessor build() {
            return new BeanProcessor(name, addBuiltinQualifiersIfNeeded(index), additionalBeanDefiningAnnotations, output, sharedAnnotationLiterals,
                    reflectionRegistration);
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

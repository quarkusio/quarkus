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

package io.quarkus.arc.processor;

import io.quarkus.arc.processor.BuildExtension.BuildContext;
import io.quarkus.arc.processor.BuildExtension.Key;
import io.quarkus.arc.processor.DeploymentEnhancer.DeploymentContext;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

/**
 *
 * @author Martin Kouba
 */
public class BeanProcessor {

    public static Builder builder() {
        return new Builder();
    }

    static final String DEFAULT_NAME = "Default";

    static final Logger LOGGER = Logger.getLogger(BeanProcessor.class);

    private final String name;

    private final IndexView index;

    private final Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations;
    private final Map<DotName, Collection<AnnotationInstance>> additionalStereotypes;

    private final ResourceOutput output;

    private final boolean sharedAnnotationLiterals;

    private final ReflectionRegistration reflectionRegistration;

    private final Collection<DotName> resourceAnnotations;

    private final List<AnnotationsTransformer> annotationTransformers;
    private final List<InjectionPointsTransformer> injectionPointsTransformers;
    private final List<BeanRegistrar> beanRegistrars;
    private final List<ContextRegistrar> contextRegistrars;
    private final List<BeanDeploymentValidator> beanDeploymentValidators;

    private final BuildContextImpl buildContext;

    private final Predicate<DotName> applicationClassPredicate;

    private final boolean removeUnusedBeans;
    private final List<Predicate<BeanInfo>> unusedExclusions;

    private BeanProcessor(String name, IndexView index, Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations,
            ResourceOutput output,
            boolean sharedAnnotationLiterals, ReflectionRegistration reflectionRegistration,
            List<AnnotationsTransformer> annotationTransformers,
            List<InjectionPointsTransformer> injectionPointsTransformers,
            Collection<DotName> resourceAnnotations, List<BeanRegistrar> beanRegistrars,
            List<ContextRegistrar> contextRegistrars, List<DeploymentEnhancer> deploymentEnhancers,
            List<BeanDeploymentValidator> beanDeploymentValidators, Predicate<DotName> applicationClassPredicate,
            boolean unusedBeansRemovalEnabled,
            List<Predicate<BeanInfo>> unusedExclusions, Map<DotName, Collection<AnnotationInstance>> additionalStereotypes) {
        this.reflectionRegistration = reflectionRegistration;
        this.applicationClassPredicate = applicationClassPredicate;
        this.name = name;
        this.additionalBeanDefiningAnnotations = additionalBeanDefiningAnnotations;
        this.additionalStereotypes = additionalStereotypes;
        this.output = Objects.requireNonNull(output);
        this.sharedAnnotationLiterals = sharedAnnotationLiterals;
        this.resourceAnnotations = resourceAnnotations;
        this.removeUnusedBeans = unusedBeansRemovalEnabled;
        this.unusedExclusions = unusedExclusions;

        // Initialize all build processors
        buildContext = new BuildContextImpl();
        buildContext.putInternal(Key.INDEX.asString(), index);

        initAndSort(deploymentEnhancers, buildContext);
        if (!deploymentEnhancers.isEmpty()) {
            Indexer indexer = new Indexer();
            DeploymentContext deploymentContext = new DeploymentContext() {

                @Override
                public void addClass(String className) {
                    BeanArchives.index(indexer, className);
                }

                @Override
                public void addClass(Class<?> clazz) {
                    BeanArchives.index(indexer, clazz.getName());
                }

                @Override
                public <V> V get(Key<V> key) {
                    return buildContext.get(key);
                }

                @Override
                public <V> V put(Key<V> key, V value) {
                    return buildContext.put(key, value);
                }
            };
            deploymentEnhancers.sort(BuildExtension::compare);
            for (DeploymentEnhancer enhancer : deploymentEnhancers) {
                enhancer.enhance(deploymentContext);
            }
            this.index = CompositeIndex.create(index, indexer.complete());
        } else {
            this.index = index;
        }

        this.annotationTransformers = initAndSort(annotationTransformers, buildContext);
        this.injectionPointsTransformers = initAndSort(injectionPointsTransformers, buildContext);
        this.beanRegistrars = initAndSort(beanRegistrars, buildContext);
        this.contextRegistrars = initAndSort(contextRegistrars, buildContext);
        this.beanDeploymentValidators = initAndSort(beanDeploymentValidators, buildContext);
    }

    public BeanDeployment process() throws IOException {

        BeanDeployment beanDeployment = new BeanDeployment(index, additionalBeanDefiningAnnotations, annotationTransformers,
                injectionPointsTransformers, resourceAnnotations, beanRegistrars, contextRegistrars, buildContext,
                removeUnusedBeans, unusedExclusions, additionalStereotypes);
        beanDeployment.init();
        beanDeployment.validate(buildContext, beanDeploymentValidators);

        PrivateMembersCollector privateMembers = new PrivateMembersCollector();
        AnnotationLiteralProcessor annotationLiterals = new AnnotationLiteralProcessor(sharedAnnotationLiterals,
                applicationClassPredicate);
        BeanGenerator beanGenerator = new BeanGenerator(annotationLiterals, applicationClassPredicate, privateMembers);
        ClientProxyGenerator clientProxyGenerator = new ClientProxyGenerator(applicationClassPredicate);
        InterceptorGenerator interceptorGenerator = new InterceptorGenerator(annotationLiterals, applicationClassPredicate,
                privateMembers);
        SubclassGenerator subclassGenerator = new SubclassGenerator(annotationLiterals, applicationClassPredicate);
        ObserverGenerator observerGenerator = new ObserverGenerator(annotationLiterals, applicationClassPredicate,
                privateMembers);
        AnnotationLiteralGenerator annotationLiteralsGenerator = new AnnotationLiteralGenerator();

        Map<BeanInfo, String> beanToGeneratedName = new HashMap<>();
        Map<ObserverInfo, String> observerToGeneratedName = new HashMap<>();

        long start = System.currentTimeMillis();
        List<Resource> resources = new ArrayList<>();

        // Generate interceptors
        for (InterceptorInfo interceptor : beanDeployment.getInterceptors()) {
            for (Resource resource : interceptorGenerator.generate(interceptor, reflectionRegistration)) {
                resources.add(resource);
                if (SpecialType.INTERCEPTOR_BEAN.equals(resource.getSpecialType())) {
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
                        resources.addAll(
                                clientProxyGenerator.generate(bean, resource.getFullyQualifiedName(), reflectionRegistration));
                    }
                    beanToGeneratedName.put(bean, resource.getName());
                    if (bean.isSubclassRequired()) {
                        resources.addAll(
                                subclassGenerator.generate(bean, resource.getFullyQualifiedName(), reflectionRegistration));
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

        privateMembers.log();

        // Generate _ComponentsProvider
        resources.addAll(
                new ComponentsProviderGenerator().generate(name, beanDeployment, beanToGeneratedName, observerToGeneratedName));

        // Generate AnnotationLiterals
        if (annotationLiterals.hasLiteralsToGenerate()) {
            resources.addAll(annotationLiteralsGenerator.generate(name, beanDeployment, annotationLiterals.getCache()));
        }

        for (Resource resource : resources) {
            output.writeResource(resource);
        }
        LOGGER.debugf("Generated %s resources in %s ms", resources.size(), System.currentTimeMillis() - start);
        return beanDeployment;
    }

    public static class Builder {

        private String name = DEFAULT_NAME;

        private IndexView index;

        private Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations = Collections.emptySet();
        private Map<DotName, Collection<AnnotationInstance>> additionalStereotypes = Collections.emptyMap();

        private ResourceOutput output;

        private boolean sharedAnnotationLiterals = true;

        private ReflectionRegistration reflectionRegistration = ReflectionRegistration.NOOP;

        private final List<DotName> resourceAnnotations = new ArrayList<>();

        private final List<AnnotationsTransformer> annotationTransformers = new ArrayList<>();
        private final List<InjectionPointsTransformer> injectionPointTransformers = new ArrayList<>();
        private final List<BeanRegistrar> beanRegistrars = new ArrayList<>();
        private final List<ContextRegistrar> contextRegistrars = new ArrayList<>();
        private final List<DeploymentEnhancer> deploymentEnhancers = new ArrayList<>();
        private final List<BeanDeploymentValidator> beanDeploymentValidators = new ArrayList<>();

        private boolean removeUnusedBeans = false;
        private final List<Predicate<BeanInfo>> removalExclusions = new ArrayList<>();

        private Predicate<DotName> applicationClassPredicate = new Predicate<DotName>() {
            @Override
            public boolean test(DotName dotName) {
                return true;
            }
        };

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setIndex(IndexView index) {
            this.index = index;
            return this;
        }

        public Builder setAdditionalBeanDefiningAnnotations(
                Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations) {
            Objects.requireNonNull(additionalBeanDefiningAnnotations);
            this.additionalBeanDefiningAnnotations = additionalBeanDefiningAnnotations;
            return this;
        }

        public Builder setAdditionalStereotypes(Map<DotName, Collection<AnnotationInstance>> additionalStereotypes) {
            Objects.requireNonNull(additionalStereotypes);
            this.additionalStereotypes = additionalStereotypes;
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

        public Builder addInjectionPointTransformer(InjectionPointsTransformer transformer) {
            this.injectionPointTransformers.add(transformer);
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

        public Builder addContextRegistrar(ContextRegistrar registrar) {
            this.contextRegistrars.add(registrar);
            return this;
        }

        public Builder addDeploymentEnhancer(DeploymentEnhancer enhancer) {
            this.deploymentEnhancers.add(enhancer);
            return this;
        }

        public Builder addBeanDeploymentValidator(BeanDeploymentValidator validator) {
            this.beanDeploymentValidators.add(validator);
            return this;
        }

        public Builder setApplicationClassPredicate(Predicate<DotName> applicationClassPredicate) {
            this.applicationClassPredicate = applicationClassPredicate;
            return this;
        }

        /**
         * If set to true the container will attempt to remove all unused beans.
         * <p>
         * An unused bean:
         * <ul>
         * <li>is not a built-in bean or interceptor,</li>
         * <li>is not eligible for injection to any injection point,</li>
         * <li>is not excluded - see {@link #addRemovalExclusion(Predicate)},</li>
         * <li>does not have a name,</li>
         * <li>does not declare an observer,</li>
         * <li>does not declare any producer which is eligible for injection to any injection point,</li>
         * <li>is not directly eligible for injection into any {@link javax.enterprise.inject.Instance} injection point</li>
         * </ul>
         *
         * @param removeUnusedBeans
         * @return
         */
        public Builder setRemoveUnusedBeans(boolean removeUnusedBeans) {
            this.removeUnusedBeans = removeUnusedBeans;
            return this;
        }

        /**
         *
         * @param exclusion
         * @return self
         * @see #setRemoveUnusedBeans(boolean)
         */
        public Builder addRemovalExclusion(Predicate<BeanInfo> exclusion) {
            this.removalExclusions.add(exclusion);
            return this;
        }

        public BeanProcessor build() {
            return new BeanProcessor(name, index, additionalBeanDefiningAnnotations, output, sharedAnnotationLiterals,
                    reflectionRegistration, annotationTransformers, injectionPointTransformers, resourceAnnotations,
                    beanRegistrars, contextRegistrars, deploymentEnhancers, beanDeploymentValidators,
                    applicationClassPredicate, removeUnusedBeans, removalExclusions, additionalStereotypes);
        }

    }

    private static <E extends BuildExtension> List<E> initAndSort(List<E> extensions, BuildContext buildContext) {
        for (Iterator<E> iterator = extensions.iterator(); iterator.hasNext();) {
            if (!iterator.next().initialize(buildContext)) {
                iterator.remove();
            }
        }
        extensions.sort(BuildExtension::compare);
        return extensions;
    }

    static class BuildContextImpl implements BuildContext {

        private final Map<String, Object> data = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        @Override
        public <V> V get(Key<V> key) {
            return (V) data.get(key.asString());
        }

        @Override
        public <V> V put(Key<V> key, V value) {
            String keyStr = key.asString();
            if (keyStr.startsWith(Key.BUILT_IN_PREFIX)) {
                throw new IllegalArgumentException("Key may not start wit " + Key.BUILT_IN_PREFIX + ": " + keyStr);
            }
            return putInternal(keyStr, value);
        }

        @SuppressWarnings("unchecked")
        <V> V putInternal(String key, V value) {
            return (V) data.put(key, value);
        }

    }

    static class PrivateMembersCollector {

        private final List<String> appDescriptions;
        private final List<String> fwkDescriptions;

        public PrivateMembersCollector() {
            this.appDescriptions = new ArrayList<>();
            this.fwkDescriptions = LOGGER.isDebugEnabled() ? new ArrayList<>() : null;
        }

        void add(boolean isApplicationClass, String description) {
            if (isApplicationClass) {
                appDescriptions.add(description);
            } else if (fwkDescriptions != null) {
                fwkDescriptions.add(description);
            }
        }

        private void log() {
            // Log application problems
            if (!appDescriptions.isEmpty()) {
                int limit = LOGGER.isDebugEnabled() ? Integer.MAX_VALUE : 3;
                String info = appDescriptions.stream().limit(limit).map(d -> "\t- " + d).collect(Collectors.joining(",\n"));
                if (appDescriptions.size() > limit) {
                    info += "\n\t- and " + (appDescriptions.size() - limit)
                            + " more - please enable debug logging to see the full list";
                }
                LOGGER.infof(
                        "Found unrecommended usage of private members (use package-private instead) in application beans:%n%s",
                        info);
            }
            // Log fwk problems
            if (fwkDescriptions != null && !fwkDescriptions.isEmpty()) {
                LOGGER.debugf(
                        "Found unrecommended usage of private members (use package-private instead) in framework beans:%n%s",
                        fwkDescriptions.stream().map(d -> "\t- " + d).collect(Collectors.joining(",\n")));
            }
        }

    }

}

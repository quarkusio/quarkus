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

import io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext;
import io.quarkus.arc.processor.BeanProcessor.BuildContextImpl;
import io.quarkus.arc.processor.BeanRegistrar.RegistrationContext;
import io.quarkus.arc.processor.BuildExtension.BuildContext;
import io.quarkus.arc.processor.BuildExtension.Key;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 *
 * @author Martin Kouba
 */
public class BeanDeployment {

    private static final Logger LOGGER = Logger.getLogger(BeanDeployment.class);

    private final IndexView index;

    private final Map<DotName, ClassInfo> qualifiers;

    private final Map<DotName, ClassInfo> interceptorBindings;

    private final Map<DotName, StereotypeInfo> stereotypes;

    private final List<BeanInfo> beans;

    private final List<InterceptorInfo> interceptors;

    private final List<ObserverInfo> observers;

    private final BeanResolver beanResolver;

    private final InterceptorResolver interceptorResolver;

    private final AnnotationStore annotationStore;

    private final Set<DotName> resourceAnnotations;

    private final List<InjectionPointInfo> injectionPoints;

    private final boolean removeUnusedBeans;
    private final List<Predicate<BeanInfo>> unusedExclusions;
    private final Set<BeanInfo> removedBeans;

    private final Map<ScopeInfo, Function<MethodCreator, ResultHandle>> customContexts;

    BeanDeployment(IndexView index, Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations,
            List<AnnotationsTransformer> annotationTransformers) {
        this(index, additionalBeanDefiningAnnotations, annotationTransformers, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null, false, null, Collections.emptyMap());
    }

    BeanDeployment(IndexView index, Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations,
            List<AnnotationsTransformer> annotationTransformers,
            Collection<DotName> resourceAnnotations, List<BeanRegistrar> beanRegistrars,
            List<ContextRegistrar> contextRegistrars,
            BuildContextImpl buildContext, boolean removeUnusedBeans, List<Predicate<BeanInfo>> unusedExclusions,
            Map<DotName, Collection<AnnotationInstance>> additionalStereotypes) {
        long start = System.currentTimeMillis();
        Collection<BeanDefiningAnnotation> beanDefiningAnnotations = new HashSet<>();
        if (additionalBeanDefiningAnnotations != null) {
            beanDefiningAnnotations.addAll(additionalBeanDefiningAnnotations);
        }
        this.resourceAnnotations = new HashSet<>(resourceAnnotations);
        this.index = index;
        this.annotationStore = new AnnotationStore(annotationTransformers, buildContext);
        this.removeUnusedBeans = removeUnusedBeans;
        this.unusedExclusions = removeUnusedBeans ? unusedExclusions : null;
        this.removedBeans = new HashSet<>();

        if (buildContext != null) {
            buildContext.putInternal(Key.ANNOTATION_STORE.asString(), annotationStore);
        }

        // Note that custom scope annotation is a bean defining annotation
        // ComponentsProviderGenerator must be aware of the custom contexts
        customContexts = new HashMap<>();
        registerCustomContexts(contextRegistrars, beanDefiningAnnotations, buildContext);

        this.qualifiers = findQualifiers(index);
        // TODO interceptor bindings are transitive!!!
        this.interceptorBindings = findInterceptorBindings(index);
        this.stereotypes = findStereotypes(index, interceptorBindings, beanDefiningAnnotations, customContexts,
                additionalStereotypes);
        this.injectionPoints = new ArrayList<>();
        this.interceptors = findInterceptors(injectionPoints);
        this.beanResolver = new BeanResolver(this);
        List<ObserverInfo> observers = new ArrayList<>();
        this.beans = findBeans(initBeanDefiningAnnotations(beanDefiningAnnotations, stereotypes.keySet()), observers,
                injectionPoints);

        if (buildContext != null) {
            buildContext.putInternal(Key.INJECTION_POINTS.asString(), Collections.unmodifiableList(injectionPoints));
            buildContext.putInternal(Key.OBSERVERS.asString(), Collections.unmodifiableList(observers));
            buildContext.putInternal(Key.BEANS.asString(), Collections.unmodifiableList(beans));
        }

        registerSyntheticBeans(beanRegistrars, buildContext);

        this.observers = observers;
        this.interceptorResolver = new InterceptorResolver(this);

        LOGGER.debugf("Bean deployment created in %s ms", System.currentTimeMillis() - start);
    }

    public Collection<BeanInfo> getBeans() {
        return Collections.unmodifiableList(beans);
    }

    public Collection<BeanInfo> getRemovedBeans() {
        return Collections.unmodifiableSet(removedBeans);
    }

    Collection<ObserverInfo> getObservers() {
        return observers;
    }

    Collection<InterceptorInfo> getInterceptors() {
        return interceptors;
    }

    IndexView getIndex() {
        return index;
    }

    BeanResolver getBeanResolver() {
        return beanResolver;
    }

    InterceptorResolver getInterceptorResolver() {
        return interceptorResolver;
    }

    ClassInfo getQualifier(DotName name) {
        return qualifiers.get(name);
    }

    ClassInfo getInterceptorBinding(DotName name) {
        return interceptorBindings.get(name);
    }

    StereotypeInfo getStereotype(DotName name) {
        return stereotypes.get(name);
    }

    Set<DotName> getResourceAnnotations() {
        return resourceAnnotations;
    }

    AnnotationStore getAnnotationStore() {
        return annotationStore;
    }

    Collection<AnnotationInstance> getAnnotations(AnnotationTarget target) {
        return annotationStore.getAnnotations(target);
    }

    AnnotationInstance getAnnotation(AnnotationTarget target, DotName name) {
        return annotationStore.getAnnotation(target, name);
    }

    Map<ScopeInfo, Function<MethodCreator, ResultHandle>> getCustomContexts() {
        return customContexts;
    }

    ScopeInfo getScope(DotName scopeAnnotationName) {
        return getScope(scopeAnnotationName, customContexts);
    }

    static ScopeInfo getScope(DotName scopeAnnotationName,
            Map<ScopeInfo, Function<MethodCreator, ResultHandle>> customContexts) {
        BuiltinScope builtin = BuiltinScope.from(scopeAnnotationName);
        if (builtin != null) {
            return builtin.getInfo();
        }
        for (ScopeInfo customScope : customContexts.keySet()) {
            if (customScope.getDotName().equals(scopeAnnotationName)) {
                return customScope;
            }
        }
        return null;
    }

    static ScopeInfo getValidScope(Collection<ScopeInfo> stereotypeScopes, AnnotationTarget target) {
        switch (stereotypeScopes.size()) {
            case 0:
                return null;
            case 1:
                return stereotypeScopes.iterator().next();
            default:
                throw new DefinitionException("All stereotypes must specify the same scope or the bean must declare a scope: "
                        + target + " declares scopes " + stereotypeScopes.stream().map(ScopeInfo::getDotName)
                                .map(DotName::toString).collect(Collectors.joining(", ")));
        }
    }

    boolean hasAnnotation(AnnotationTarget target, DotName name) {
        return annotationStore.hasAnnotation(target, name);
    }

    void validate(BuildContextImpl buildContext, List<BeanDeploymentValidator> validators) {
        long start = System.currentTimeMillis();
        // Validate the bean deployment
        List<Throwable> errors = new ArrayList<>();
        validateBeans(errors);
        ValidationContextImpl validationContext = new ValidationContextImpl(buildContext);
        for (BeanDeploymentValidator validator : validators) {
            validator.validate(validationContext);
        }
        errors.addAll(validationContext.getErrors());
        processErrors(errors);
        LOGGER.debugf("Bean deployment validated in %s ms", System.currentTimeMillis() - start);
    }

    void init() {
        long start = System.currentTimeMillis();

        // Collect dependency resolution errors
        List<Throwable> errors = new ArrayList<>();
        for (BeanInfo bean : beans) {
            bean.init(errors);
        }
        for (ObserverInfo observer : observers) {
            observer.init(errors);
        }
        for (InterceptorInfo interceptor : interceptors) {
            interceptor.init(errors);
        }
        processErrors(errors);

        if (removeUnusedBeans) {
            long removalStart = System.currentTimeMillis();
            Set<BeanInfo> removable = new HashSet<>();
            Set<BeanInfo> unusedProducers = new HashSet<>();
            List<BeanInfo> producers = beans.stream().filter(b -> b.isProducerMethod() || b.isProducerField())
                    .collect(Collectors.toList());
            List<InjectionPointInfo> instanceInjectionPoints = injectionPoints.stream()
                    .filter(ip -> BuiltinBean.resolve(ip) == BuiltinBean.INSTANCE)
                    .collect(Collectors.toList());
            for (BeanInfo bean : beans) {
                // Named beans can be used in templates and expressions
                if (bean.getName() != null) {
                    continue;
                }
                // Custom exclusions
                if (unusedExclusions.stream().anyMatch(e -> e.test(bean))) {
                    continue;
                }
                // Is injected
                if (injectionPoints.stream().anyMatch(ip -> bean.equals(ip.getResolvedBean()))) {
                    continue;
                }
                // Declares an observer method
                if (observers.stream().anyMatch((o) -> bean.equals(o.getDeclaringBean()))) {
                    continue;
                }
                // Declares a producer - see also second pass
                if (producers.stream().anyMatch(b -> bean.equals(b.getDeclaringBean()))) {
                    continue;
                }
                // Instance<Foo>
                if (instanceInjectionPoints.stream()
                        .anyMatch(ip -> Beans.matchesType(bean, ip.getRequiredType().asParameterizedType().arguments().get(0))
                                && ip.getRequiredQualifiers().stream().allMatch(q -> Beans.hasQualifier(bean, q)))) {
                    continue;
                }
                if (bean.isProducerField() || bean.isProducerMethod()) {
                    // This bean is very likely an unused producer
                    unusedProducers.add(bean);
                }
                removable.add(bean);
            }
            if (!unusedProducers.isEmpty()) {
                // Second pass to find beans which themselves are unused and declare only unused producers
                Map<BeanInfo, List<BeanInfo>> declaringMap = producers.stream()
                        .collect(Collectors.groupingBy(BeanInfo::getDeclaringBean));
                for (Entry<BeanInfo, List<BeanInfo>> entry : declaringMap.entrySet()) {
                    if (unusedProducers.containsAll(entry.getValue())) {
                        // All producers declared by this bean are unused
                        removable.add(entry.getKey());
                    }
                }
            }
            if (!removable.isEmpty()) {
                beans.removeAll(removable);
                removedBeans.addAll(removable);
                removedBeans.forEach(b -> LOGGER.debugf("Removed unused %s", b));
            }
            LOGGER.debugf("Removed %s unused beans in %s ms", removable.size(), System.currentTimeMillis() - removalStart);
        }
        LOGGER.debugf("Bean deployment initialized in %s ms", System.currentTimeMillis() - start);
    }

    static Map<DotName, ClassInfo> findQualifiers(IndexView index) {
        Map<DotName, ClassInfo> qualifiers = new HashMap<>();
        for (AnnotationInstance qualifier : index.getAnnotations(DotNames.QUALIFIER)) {
            qualifiers.put(qualifier.target().asClass().name(), qualifier.target().asClass());
        }
        return qualifiers;
    }

    static Map<DotName, ClassInfo> findInterceptorBindings(IndexView index) {
        Map<DotName, ClassInfo> bindings = new HashMap<>();
        for (AnnotationInstance binding : index.getAnnotations(DotNames.INTERCEPTOR_BINDING)) {
            bindings.put(binding.target().asClass().name(), binding.target().asClass());
        }
        return bindings;
    }

    Map<DotName, StereotypeInfo> findStereotypes(IndexView index, Map<DotName, ClassInfo> interceptorBindings,
            Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations,
            Map<ScopeInfo, Function<MethodCreator, ResultHandle>> customContexts,
            Map<DotName, Collection<AnnotationInstance>> additionalStereotypes) {
        Map<DotName, StereotypeInfo> stereotypes = new HashMap<>();
        final List<AnnotationInstance> stereotypeAnnotations = new ArrayList<>(index.getAnnotations(DotNames.STEREOTYPE));
        for (final Collection<AnnotationInstance> annotations : additionalStereotypes.values()) {
            stereotypeAnnotations.addAll(annotations);
        }
        for (AnnotationInstance stereotype : stereotypeAnnotations) {
            final DotName stereotypeName = stereotype.target().asClass().name();
            ClassInfo stereotypeClass = index.getClassByName(stereotypeName);
            if (stereotypeClass != null) {

                boolean isAlternative = false;
                List<ScopeInfo> scopes = new ArrayList<>();
                List<AnnotationInstance> bindings = new ArrayList<>();
                boolean isNamed = false;

                for (AnnotationInstance annotation : getAnnotations(stereotypeClass)) {
                    if (DotNames.ALTERNATIVE.equals(annotation.name())) {
                        isAlternative = true;
                    } else if (interceptorBindings.containsKey(annotation.name())) {
                        bindings.add(annotation);
                    } else if (DotNames.NAMED.equals(annotation.name())) {
                        if (annotation.value() != null && !annotation.value()
                                .asString()
                                .isEmpty()) {
                            throw new DefinitionException(
                                    "Stereotype must not declare @Named with a non-empty value: " + stereotypeClass);
                        }
                        isNamed = true;
                    } else {
                        final ScopeInfo scope = getScope(annotation.name(), customContexts);
                        if (scope != null) {
                            scopes.add(scope);
                        }
                    }
                }
                final ScopeInfo scope = getValidScope(scopes, stereotypeClass);
                stereotypes.put(stereotypeName, new StereotypeInfo(scope, bindings, isAlternative, isNamed, stereotypeClass));
            }
        }
        //if an additional bean defining annotation has a default scope we register it as a stereotype
        if (additionalBeanDefiningAnnotations != null) {
            for (BeanDefiningAnnotation i : additionalBeanDefiningAnnotations) {
                if (i.getDefaultScope() != null) {
                    ScopeInfo scope = getScope(i.getDefaultScope(), customContexts);
                    stereotypes.put(i.getAnnotation(), new StereotypeInfo(scope, Collections.emptyList(), false, false,
                            index.getClassByName(i.getAnnotation())));
                }
            }
        }

        return stereotypes;
    }

    private void registerSyntheticBeans(List<BeanRegistrar> beanRegistrars, BuildContext buildContext) {
        if (!beanRegistrars.isEmpty()) {
            RegistrationContext registrationContext = new RegistrationContext() {

                @Override
                public <T> BeanConfigurator<T> configure(DotName beanClassName) {
                    return new BeanConfigurator<T>(beanClassName, BeanDeployment.this, beans::add);
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
            for (BeanRegistrar registrar : beanRegistrars) {
                registrar.register(registrationContext);
            }
        }
    }

    private void registerCustomContexts(List<ContextRegistrar> contextRegistrars,
            Collection<BeanDefiningAnnotation> beanDefiningAnnotations, BuildContext buildContext) {
        if (!contextRegistrars.isEmpty()) {
            io.quarkus.arc.processor.ContextRegistrar.RegistrationContext registrationContext = new io.quarkus.arc.processor.ContextRegistrar.RegistrationContext() {
                @Override
                public <V> V put(Key<V> key, V value) {
                    return buildContext.put(key, value);
                }

                @Override
                public <V> V get(Key<V> key) {
                    return buildContext.get(key);
                }

                @Override
                public ContextConfigurator configure(Class<? extends Annotation> scopeAnnotation) {
                    return new ContextConfigurator(scopeAnnotation,
                            c -> {
                                ScopeInfo scope = new ScopeInfo(c.scopeAnnotation, c.isNormal);
                                beanDefiningAnnotations.add(new BeanDefiningAnnotation(scope.getDotName(), null));
                                customContexts.put(scope, c.creator);
                            });
                }
            };
            for (ContextRegistrar contextRegistrar : contextRegistrars) {
                contextRegistrar.register(registrationContext);
            }
        }
    }

    private void validateBeans(List<Throwable> errors) {
        Map<String, List<BeanInfo>> namedBeans = new HashMap<>();

        for (BeanInfo bean : beans) {
            if (bean.getName() != null) {
                List<BeanInfo> named = namedBeans.get(bean.getName());
                if (named == null) {
                    named = new ArrayList<>();
                    namedBeans.put(bean.getName(), named);
                }
                named.add(bean);
            }
            bean.validate(errors);
        }

        if (!namedBeans.isEmpty()) {
            for (Entry<String, List<BeanInfo>> entry : namedBeans.entrySet()) {
                if (entry.getValue()
                        .size() > 1) {
                    if (Beans.resolveAmbiguity(entry.getValue()) == null) {
                        errors.add(new DeploymentException("Unresolvable ambiguous bean name detected: " + entry.getKey()
                                + "\nBeans:\n" + entry.getValue()
                                        .stream()
                                        .map(Object::toString)
                                        .collect(Collectors.joining("\n"))));
                    }
                }
            }
        }
    }

    private List<BeanInfo> findBeans(Collection<DotName> beanDefiningAnnotations, List<ObserverInfo> observers,
            List<InjectionPointInfo> injectionPoints) {

        Set<ClassInfo> beanClasses = new HashSet<>();
        Set<MethodInfo> producerMethods = new HashSet<>();
        Set<MethodInfo> disposerMethods = new HashSet<>();
        Set<FieldInfo> producerFields = new HashSet<>();
        Set<MethodInfo> syncObserverMethods = new HashSet<>();
        Set<MethodInfo> asyncObserverMethods = new HashSet<>();

        for (ClassInfo beanClass : index.getKnownClasses()) {

            if (Modifier.isInterface(beanClass.flags()) || DotNames.ENUM.equals(beanClass.superName())) {
                // Skip interfaces, annotations and enums
                continue;
            }

            if (beanClass.nestingType().equals(NestingType.ANONYMOUS) || beanClass.nestingType().equals(NestingType.LOCAL)
                    || (beanClass.nestingType().equals(NestingType.INNER) && !Modifier.isStatic(beanClass.flags()))) {
                // Skip anonymous, local and inner classes
                continue;
            }

            if (!beanClass.hasNoArgsConstructor()) {
                int numberOfConstructorsWithoutInject = 0;
                int numberOfConstructorsWithInject = 0;
                for (MethodInfo m : beanClass.methods()) {
                    if (m.name().equals(Methods.INIT)) {
                        if (m.hasAnnotation(DotNames.INJECT)) {
                            numberOfConstructorsWithInject++;
                        } else {
                            numberOfConstructorsWithoutInject++;
                        }
                    }
                }

                // a bean without no-arg constructor needs to have either a constructor annotated with @Inject
                // or a single constructor
                if (numberOfConstructorsWithInject == 0 && numberOfConstructorsWithoutInject != 1) {
                    continue;
                }
            }

            if (annotationStore.hasAnnotation(beanClass, DotNames.VETOED)) {
                // Skip vetoed bean classes
                continue;
            }

            if (annotationStore.hasAnnotation(beanClass, DotNames.INTERCEPTOR)) {
                // Skip interceptors
                continue;
            }

            if (beanClass.interfaceNames().contains(DotNames.EXTENSION)) {
                // Skip portable extensions
                continue;
            }

            boolean hasBeanDefiningAnnotation = false;
            if (annotationStore.hasAnyAnnotation(beanClass, beanDefiningAnnotations)) {
                hasBeanDefiningAnnotation = true;
                beanClasses.add(beanClass);
            }

            for (MethodInfo method : beanClass.methods()) {
                if (annotationStore.getAnnotations(method).isEmpty()) {
                    continue;
                }
                if (annotationStore.hasAnnotation(method, DotNames.PRODUCES)) {
                    // Producers are not inherited
                    producerMethods.add(method);
                    if (!hasBeanDefiningAnnotation) {
                        LOGGER.debugf("Producer method found but %s has no bean defining annotation - using @Dependent",
                                beanClass);
                        beanClasses.add(beanClass);
                    }
                } else if (annotationStore.hasAnnotation(method, DotNames.DISPOSES)) {
                    // Disposers are not inherited
                    disposerMethods.add(method);
                } else if (annotationStore.hasAnnotation(method, DotNames.OBSERVES)) {
                    // TODO observers are inherited
                    syncObserverMethods.add(method);
                    if (!hasBeanDefiningAnnotation) {
                        LOGGER.debugf("Observer method found but %s has no bean defining annotation - using @Dependent",
                                beanClass);
                        beanClasses.add(beanClass);
                    }
                } else if (annotationStore.hasAnnotation(method, DotNames.OBSERVES_ASYNC)) {
                    // TODO observers are inherited
                    asyncObserverMethods.add(method);
                    if (!hasBeanDefiningAnnotation) {
                        LOGGER.debugf("Observer method found but %s has no bean defining annotation - using @Dependent",
                                beanClass);
                        beanClasses.add(beanClass);
                    }
                }
            }
            for (FieldInfo field : beanClass.fields()) {
                if (annotationStore.hasAnnotation(field, DotNames.PRODUCES)) {
                    // Producer fields are not inherited
                    producerFields.add(field);
                    if (!hasBeanDefiningAnnotation) {
                        LOGGER.debugf("Producer field found but %s has no bean defining annotation - using @Dependent",
                                beanClass);
                        beanClasses.add(beanClass);
                    }
                }
            }
        }

        // Build metadata for typesafe resolution
        List<BeanInfo> beans = new ArrayList<>();
        Map<ClassInfo, BeanInfo> beanClassToBean = new HashMap<>();
        for (ClassInfo beanClass : beanClasses) {
            BeanInfo classBean = Beans.createClassBean(beanClass, this);
            beans.add(classBean);
            beanClassToBean.put(beanClass, classBean);
            injectionPoints.addAll(classBean.getAllInjectionPoints());
        }

        List<DisposerInfo> disposers = new ArrayList<>();
        for (MethodInfo disposerMethod : disposerMethods) {
            BeanInfo declaringBean = beanClassToBean.get(disposerMethod.declaringClass());
            if (declaringBean != null) {
                Injection injection = Injection.forDisposer(disposerMethod, this);
                disposers.add(new DisposerInfo(declaringBean, disposerMethod, injection));
                injectionPoints.addAll(injection.injectionPoints);
            }
        }

        for (MethodInfo producerMethod : producerMethods) {
            BeanInfo declaringBean = beanClassToBean.get(producerMethod.declaringClass());
            if (declaringBean != null) {
                BeanInfo producerMethodBean = Beans.createProducerMethod(producerMethod, declaringBean, this,
                        findDisposer(declaringBean, producerMethod, disposers));
                beans.add(producerMethodBean);
                injectionPoints.addAll(producerMethodBean.getAllInjectionPoints());
            }
        }

        for (FieldInfo producerField : producerFields) {
            BeanInfo declaringBean = beanClassToBean.get(producerField.declaringClass());
            if (declaringBean != null) {
                beans.add(Beans.createProducerField(producerField, declaringBean, this,
                        findDisposer(declaringBean, producerField, disposers)));
            }
        }

        for (MethodInfo observerMethod : syncObserverMethods) {
            BeanInfo declaringBean = beanClassToBean.get(observerMethod.declaringClass());
            if (declaringBean != null) {
                Injection injection = Injection.forObserver(observerMethod, this);
                observers.add(new ObserverInfo(declaringBean, observerMethod, injection, false));
                injectionPoints.addAll(injection.injectionPoints);
            }
        }
        for (MethodInfo observerMethod : asyncObserverMethods) {
            BeanInfo declaringBean = beanClassToBean.get(observerMethod.declaringClass());
            if (declaringBean != null) {
                Injection injection = Injection.forObserver(observerMethod, this);
                observers.add(new ObserverInfo(declaringBean, observerMethod, injection, true));
                injectionPoints.addAll(injection.injectionPoints);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            for (BeanInfo bean : beans) {
                LOGGER.logf(Level.TRACE, "Created %s", bean);
            }
        }
        return beans;
    }

    private DisposerInfo findDisposer(BeanInfo declaringBean, AnnotationTarget annotationTarget, List<DisposerInfo> disposers) {
        List<DisposerInfo> found = new ArrayList<>();
        Type beanType;
        Set<AnnotationInstance> qualifiers;
        if (Kind.FIELD.equals(annotationTarget.kind())) {
            beanType = annotationTarget.asField().type();
            qualifiers = annotationTarget.asField().annotations().stream().filter(a -> getQualifier(a.name()) != null)
                    .collect(Collectors.toSet());
        } else if (Kind.METHOD.equals(annotationTarget.kind())) {
            beanType = annotationTarget.asMethod().returnType();
            qualifiers = annotationTarget.asMethod().annotations().stream()
                    .filter(a -> Kind.METHOD.equals(a.target().kind()) && getQualifier(a.name()) != null)
                    .collect(Collectors.toSet());
        } else {
            throw new RuntimeException("Unsupported annotation target: " + annotationTarget);
        }
        for (DisposerInfo disposer : disposers) {
            if (disposer.getDeclaringBean().equals(declaringBean)) {
                boolean hasQualifier = true;
                for (AnnotationInstance qualifier : qualifiers) {
                    if (!Beans.hasQualifier(getQualifier(qualifier.name()), qualifier,
                            disposer.getDisposedParameteterQualifiers())) {
                        hasQualifier = false;
                    }
                }
                if (hasQualifier && beanResolver.matches(beanType, disposer.getDiposedParameterType())) {
                    found.add(disposer);
                }
            }
        }
        if (found.size() > 1) {
            throw new DefinitionException("Multiple disposer methods found for " + annotationTarget);
        }
        return found.isEmpty() ? null : found.get(0);
    }

    private List<InterceptorInfo> findInterceptors(List<InjectionPointInfo> injectionPoints) {
        Set<ClassInfo> interceptorClasses = new HashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(DotNames.INTERCEPTOR)) {
            if (Kind.CLASS.equals(annotation.target().kind())) {
                interceptorClasses.add(annotation.target().asClass());
            }
        }
        List<InterceptorInfo> interceptors = new ArrayList<>();
        for (ClassInfo interceptorClass : interceptorClasses) {
            interceptors.add(Interceptors.createInterceptor(interceptorClass, this));
        }
        if (LOGGER.isTraceEnabled()) {
            for (InterceptorInfo interceptor : interceptors) {
                LOGGER.logf(Level.TRACE, "Created %s", interceptor);
            }
        }
        for (InterceptorInfo i : interceptors) {
            injectionPoints.addAll(i.getAllInjectionPoints());
        }
        return interceptors;
    }

    private void processErrors(List<Throwable> errors) {
        if (!errors.isEmpty()) {
            if (errors.size() == 1) {
                Throwable error = errors.get(0);
                if (error instanceof DeploymentException) {
                    throw (DeploymentException) error;
                } else {
                    throw new DeploymentException(errors.get(0));
                }
            } else {
                StringBuilder message = new StringBuilder("Found " + errors.size() + " deployment problems: ");
                int idx = 1;
                for (Throwable error : errors) {
                    message.append("\n").append("[").append(idx++).append("] ").append(error.getMessage());
                }
                DeploymentException deploymentException = new DeploymentException(message.toString());
                for (Throwable error : errors) {
                    deploymentException.addSuppressed(error);
                }
                throw deploymentException;
            }
        }
    }

    public static Set<DotName> initBeanDefiningAnnotations(Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations,
            Set<DotName> stereotypes) {
        Set<DotName> beanDefiningAnnotations = new HashSet<>();
        for (BuiltinScope scope : BuiltinScope.values()) {
            beanDefiningAnnotations.add(scope.getInfo().getDotName());
        }
        if (additionalBeanDefiningAnnotations != null) {
            for (BeanDefiningAnnotation additional : additionalBeanDefiningAnnotations) {
                beanDefiningAnnotations.add(additional.getAnnotation());
            }
        }
        beanDefiningAnnotations.addAll(stereotypes);
        beanDefiningAnnotations.add(DotNames.create(Model.class));
        return beanDefiningAnnotations;
    }

    static class ValidationContextImpl implements ValidationContext {

        private final BuildContext buildContext;

        private final List<Throwable> errors;

        public ValidationContextImpl(BuildContext buildContext) {
            this.buildContext = buildContext;
            this.errors = new ArrayList<Throwable>();
        }

        @Override
        public <V> V get(Key<V> key) {
            return buildContext.get(key);
        }

        @Override
        public <V> V put(Key<V> key, V value) {
            return buildContext.put(key, value);
        }

        @Override
        public void addDeploymentProblem(Throwable problem) {
            errors.add(problem);
        }

        List<Throwable> getErrors() {
            return errors;
        }

    }

}

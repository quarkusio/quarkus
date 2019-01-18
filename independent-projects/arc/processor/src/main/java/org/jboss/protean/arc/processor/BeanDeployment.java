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

package org.jboss.protean.arc.processor;

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
import org.jboss.protean.arc.processor.BeanDeploymentValidator.ValidationContext;
import org.jboss.protean.arc.processor.BeanProcessor.BuildContextImpl;
import org.jboss.protean.arc.processor.BeanRegistrar.RegistrationContext;
import org.jboss.protean.arc.processor.BuildExtension.BuildContext;
import org.jboss.protean.arc.processor.BuildExtension.Key;

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

    BeanDeployment(IndexView index, Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations, List<AnnotationsTransformer> annotationTransformers) {
        this(index, additionalBeanDefiningAnnotations, annotationTransformers, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null);
    }

    BeanDeployment(IndexView index, Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations, List<AnnotationsTransformer> annotationTransformers,
            Collection<DotName> resourceAnnotations, List<BeanRegistrar> beanRegistrars, List<BeanDeploymentValidator> validators,
            BuildContextImpl buildContext) {
        long start = System.currentTimeMillis();
        this.resourceAnnotations = new HashSet<>(resourceAnnotations);
        this.index = index;
        this.annotationStore = new AnnotationStore(annotationTransformers, buildContext);

        if (buildContext != null) {
            buildContext.putInternal(Key.ANNOTATION_STORE.asString(), annotationStore);
        }

        this.qualifiers = findQualifiers(index);
        // TODO interceptor bindings are transitive!!!
        this.interceptorBindings = findInterceptorBindings(index);
        this.stereotypes = findStereotypes(index, interceptorBindings, additionalBeanDefiningAnnotations);
        this.interceptors = findInterceptors();
        this.beanResolver = new BeanResolver(this);
        List<ObserverInfo> observers = new ArrayList<>();
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        this.beans = findBeans(initBeanDefiningAnnotations(additionalBeanDefiningAnnotations, stereotypes.keySet()), observers, injectionPoints);
        
        if (buildContext != null) {
            buildContext.putInternal(Key.INJECTION_POINTS.asString(), Collections.unmodifiableList(injectionPoints));
            buildContext.putInternal(Key.OBSERVERS.asString(), Collections.unmodifiableList(observers));
            buildContext.putInternal(Key.BEANS.asString(), Collections.unmodifiableList(beans));
        }

        // Register synthetic beans
        if (!beanRegistrars.isEmpty()) {
            RegistrationContext registrationContext = new RegistrationContext() {

                @Override
                public <T> BeanConfigurator<T> configure(Class<?> beanClass) {
                    return new BeanConfigurator<T>(beanClass, BeanDeployment.this, beans::add);
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

        // Validate the bean deployment
        List<Throwable> errors = new ArrayList<>();
        validateBeanNames(errors);
        ValidationContextImpl validationContext = new ValidationContextImpl(buildContext);
        for (BeanDeploymentValidator validator : validators) {
            validator.validate(validationContext);
        }
        errors.addAll(validationContext.getErrors());

        if (!errors.isEmpty()) {
            if (errors.size() == 1) {
                Throwable error = errors.get(0);
                if (error instanceof DeploymentException) {
                    throw (DeploymentException) error;
                } else {
                    throw new DeploymentException(errors.get(0));
                }
            } else {
                DeploymentException deploymentException = new DeploymentException("Multiple deployment problems occured: " + errors.stream()
                        .map(e -> e.getMessage())
                        .collect(Collectors.toList())
                        .toString());
                for (Throwable error : errors) {
                    deploymentException.addSuppressed(error);
                }
                throw deploymentException;
            }
        }

        this.observers = observers;
        this.interceptorResolver = new InterceptorResolver(this);

        LOGGER.debugf("Bean deployment created in %s ms", System.currentTimeMillis() - start);
    }
    
    private void validateBeanNames(List<Throwable> errors) {
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
        }
        if (!namedBeans.isEmpty()) {
            for (Entry<String, List<BeanInfo>> entry : namedBeans.entrySet()) {
                if (entry.getValue()
                        .size() > 1) {
                    if (Beans.resolveAmbiguity(entry.getValue()) == null) {
                        errors.add(new DeploymentException("Unresolvable ambiguous bean name detected: " + entry.getKey() + "\nBeans:\n" + entry.getValue()
                                .stream()
                                .map(Object::toString)
                                .collect(Collectors.joining("\n"))));
                    }
                }
            }
        }
    }
    
    public Collection<BeanInfo> getBeans() {
        return beans;
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

    boolean hasAnnotation(AnnotationTarget target, DotName name) {
        return annotationStore.hasAnnotation(target, name);
    }

    void init() {
        long start = System.currentTimeMillis();
        beans.forEach(BeanInfo::init);
        observers.forEach(ObserverInfo::init);
        interceptors.forEach(InterceptorInfo::init);
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

    static Map<DotName, StereotypeInfo> findStereotypes(IndexView index, Map<DotName, ClassInfo> interceptorBindings, Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations) {
        Map<DotName, StereotypeInfo> stereotypes = new HashMap<>();
        for (AnnotationInstance stereotype : index.getAnnotations(DotNames.STEREOTYPE)) {
            ClassInfo stereotypeClass = index.getClassByName(stereotype.target().asClass().name());
            if (stereotypeClass != null) {

                boolean isAlternative = false;
                ScopeInfo scope = null;
                List<AnnotationInstance> bindings = new ArrayList<>();
                boolean isNamed = false;

                for (AnnotationInstance annotation : stereotypeClass.classAnnotations()) {
                    if (DotNames.ALTERNATIVE.equals(annotation.name())) {
                        isAlternative = true;
                    } else if (interceptorBindings.containsKey(annotation.name())) {
                        bindings.add(annotation);
                    } else if (DotNames.NAMED.equals(annotation.name())) {
                        if (annotation.value() != null && !annotation.value()
                                .asString()
                                .isEmpty()) {
                            throw new DefinitionException("Stereotype must not declare @Named with a non-empty value: " + stereotypeClass);
                        }
                        isNamed = true;
                    } else if (scope == null) {
                        scope = ScopeInfo.from(annotation.name());
                    }
                }
                stereotypes.put(stereotype.target().asClass().name(), new StereotypeInfo(scope, bindings, isAlternative, isNamed, stereotypeClass));
            }
        }
        //if an additional bean defining annotation has a default scope we register it as a stereotype
        if(additionalBeanDefiningAnnotations != null) {
            for (BeanDefiningAnnotation i : additionalBeanDefiningAnnotations) {
                if (i.getDefaultScope() != null) {
                    stereotypes.put(i.getAnnotation(), new StereotypeInfo(ScopeInfo.from(i.getDefaultScope()), Collections.emptyList(), false, false, index.getClassByName(i.getAnnotation())));
                }
            }
        }


        return stereotypes;
    }

    private List<BeanInfo> findBeans(Collection<DotName> beanDefiningAnnotations, List<ObserverInfo> observers, List<InjectionPointInfo> injectionPoints) {

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
                // Skip annonymous, local and inner classes
                continue;
            }

            if (!beanClass.hasNoArgsConstructor()) {
                int numberOfConstructorsWithoutInject = 0;
                int numberOfConstructorsWithInject = 0;
                for (MethodInfo m : beanClass.methods()) {
                    if (m.name().equals("<init>")) {
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
                        LOGGER.debugf("Producer method found but %s has no bean defining annotation - using @Dependent", beanClass);
                        beanClasses.add(beanClass);
                    }
                } else if (annotationStore.hasAnnotation(method, DotNames.DISPOSES)) {
                    // Disposers are not inherited
                    disposerMethods.add(method);
                } else if (annotationStore.hasAnnotation(method, DotNames.OBSERVES)) {
                    // TODO observers are inherited
                    syncObserverMethods.add(method);
                    if (!hasBeanDefiningAnnotation) {
                        LOGGER.debugf("Observer method found but %s has no bean defining annotation - using @Dependent", beanClass);
                        beanClasses.add(beanClass);
                    }
                } else if (annotationStore.hasAnnotation(method, DotNames.OBSERVES_ASYNC)) {
                    // TODO observers are inherited
                    asyncObserverMethods.add(method);
                    if (!hasBeanDefiningAnnotation) {
                        LOGGER.debugf("Observer method found but %s has no bean defining annotation - using @Dependent", beanClass);
                        beanClasses.add(beanClass);
                    }
                }
            }
            for (FieldInfo field : beanClass.fields()) {
                if (annotationStore.hasAnnotation(field, DotNames.PRODUCES)) {
                    // Producer fields are not inherited
                    producerFields.add(field);
                    if (!hasBeanDefiningAnnotation) {
                        LOGGER.debugf("Producer field found but %s has no bean defining annotation - using @Dependent", beanClass);
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
                beans.add(Beans.createProducerField(producerField, declaringBean, this, findDisposer(declaringBean, producerField, disposers)));
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

        if (LOGGER.isDebugEnabled()) {
            for (BeanInfo bean : beans) {
                LOGGER.logf(Level.DEBUG, "Created %s", bean);
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
            qualifiers = annotationTarget.asField().annotations().stream().filter(a -> getQualifier(a.name()) != null).collect(Collectors.toSet());
        } else if (Kind.METHOD.equals(annotationTarget.kind())) {
            beanType = annotationTarget.asMethod().returnType();
            qualifiers = annotationTarget.asMethod().annotations().stream().filter(a -> Kind.METHOD.equals(a.target().kind()) && getQualifier(a.name()) != null)
                    .collect(Collectors.toSet());
        } else {
            throw new RuntimeException("Unsupported annotation target: " + annotationTarget);
        }
        for (DisposerInfo disposer : disposers) {
            if (disposer.getDeclaringBean().equals(declaringBean)) {
                boolean hasQualifier = true;
                for (AnnotationInstance qualifier : qualifiers) {
                    if (!Beans.hasQualifier(getQualifier(qualifier.name()), qualifier, null)) {
                        hasQualifier = false;
                    }
                }
                if (hasQualifier && beanResolver.matches(beanType, disposer.getDisposerMethod().parameters().get(disposer.getDisposedParameter().position()))) {
                    found.add(disposer);
                }

            }
        }
        if (found.size() > 1) {
            throw new DefinitionException("Multiple disposer methods found for " + annotationTarget);
        }
        return found.isEmpty() ? null : found.get(0);
    }

    private List<InterceptorInfo> findInterceptors() {
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
        if (LOGGER.isDebugEnabled()) {
            for (InterceptorInfo interceptor : interceptors) {
                LOGGER.logf(Level.DEBUG, "Created %s", interceptor);
            }
        }
        return interceptors;
    }

    public static Set<DotName> initBeanDefiningAnnotations(Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations, Set<DotName> stereotypes) {
        Set<DotName> beanDefiningAnnotations = new HashSet<>();
        for (ScopeInfo scope : ScopeInfo.values()) {
            beanDefiningAnnotations.add(scope.getDotName());
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

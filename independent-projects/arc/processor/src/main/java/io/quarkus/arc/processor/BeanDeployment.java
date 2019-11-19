package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
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

public class BeanDeployment {

    private static final Logger LOGGER = Logger.getLogger(BeanDeployment.class);
    public static final EnumSet<Type.Kind> CLASS_TYPES = EnumSet.of(Type.Kind.CLASS, Type.Kind.PARAMETERIZED_TYPE);

    private final BuildContextImpl buildContext;

    private final IndexView index;

    private final Map<DotName, ClassInfo> qualifiers;

    private final Map<DotName, ClassInfo> interceptorBindings;

    private final Map<DotName, Set<String>> nonBindingFields;

    private final Map<DotName, Set<AnnotationInstance>> transitiveInterceptorBindings;

    private final Map<DotName, StereotypeInfo> stereotypes;

    private final List<BeanInfo> beans;

    private final List<InterceptorInfo> interceptors;

    private final List<ObserverInfo> observers;

    private final BeanResolver beanResolver;

    private final InterceptorResolver interceptorResolver;

    private final AnnotationStore annotationStore;

    private final InjectionPointModifier injectionPointTransformer;

    private final Set<DotName> resourceAnnotations;

    private final List<InjectionPointInfo> injectionPoints;

    private final boolean removeUnusedBeans;
    private final List<Predicate<BeanInfo>> unusedExclusions;
    private final Set<BeanInfo> removedBeans;

    private final Map<ScopeInfo, Function<MethodCreator, ResultHandle>> customContexts;

    private final Collection<BeanDefiningAnnotation> beanDefiningAnnotations;
    private final boolean removeFinalForProxyableMethods;

    BeanDeployment(IndexView index, Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations,
            List<AnnotationsTransformer> annotationTransformers) {
        this(index, additionalBeanDefiningAnnotations, annotationTransformers, Collections.emptyList(), Collections.emptyList(),
                null, false, null, Collections.emptyMap(), Collections.emptyList(), false);
    }

    BeanDeployment(IndexView index, Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations,
            List<AnnotationsTransformer> annotationTransformers,
            List<InjectionPointsTransformer> injectionPointsTransformers,
            Collection<DotName> resourceAnnotations,
            BuildContextImpl buildContext, boolean removeUnusedBeans, List<Predicate<BeanInfo>> unusedExclusions,
            Map<DotName, Collection<AnnotationInstance>> additionalStereotypes,
            List<InterceptorBindingRegistrar> bindingRegistrars, boolean removeFinalForProxyableMethods) {
        this.buildContext = buildContext;
        Set<BeanDefiningAnnotation> beanDefiningAnnotations = new HashSet<>();
        if (additionalBeanDefiningAnnotations != null) {
            beanDefiningAnnotations.addAll(additionalBeanDefiningAnnotations);
        }
        this.beanDefiningAnnotations = beanDefiningAnnotations;
        this.resourceAnnotations = new HashSet<>(resourceAnnotations);
        this.index = index;
        this.annotationStore = new AnnotationStore(annotationTransformers, buildContext);
        if (buildContext != null) {
            buildContext.putInternal(Key.ANNOTATION_STORE.asString(), annotationStore);
        }
        this.injectionPointTransformer = new InjectionPointModifier(injectionPointsTransformers, buildContext);
        this.removeUnusedBeans = removeUnusedBeans;
        this.unusedExclusions = removeUnusedBeans ? unusedExclusions : null;
        this.removedBeans = new CopyOnWriteArraySet<>();

        this.customContexts = new ConcurrentHashMap<>();

        this.qualifiers = findQualifiers(index);
        buildContextPut(Key.QUALIFIERS.asString(), Collections.unmodifiableMap(qualifiers));

        this.interceptorBindings = findInterceptorBindings(index);
        this.nonBindingFields = new HashMap<>();
        for (InterceptorBindingRegistrar registrar : bindingRegistrars) {
            for (Map.Entry<DotName, Set<String>> bindingEntry : registrar.registerAdditionalBindings().entrySet()) {
                DotName dotName = bindingEntry.getKey();
                ClassInfo classInfo = getClassByName(index, dotName);
                if (classInfo != null) {
                    if (bindingEntry.getValue() != null) {
                        nonBindingFields.put(dotName, bindingEntry.getValue());
                    }
                    interceptorBindings.put(dotName, classInfo);
                }
            }
        }
        buildContextPut(Key.INTERCEPTOR_BINDINGS.asString(), Collections.unmodifiableMap(interceptorBindings));

        this.stereotypes = findStereotypes(index, interceptorBindings, beanDefiningAnnotations, customContexts,
                additionalStereotypes, annotationStore);
        buildContextPut(Key.STEREOTYPES.asString(), Collections.unmodifiableMap(stereotypes));

        this.transitiveInterceptorBindings = findTransitiveInterceptorBindigs(interceptorBindings.keySet(), index,
                new HashMap<>(), interceptorBindings, annotationStore);

        this.injectionPoints = new CopyOnWriteArrayList<>();
        this.interceptors = new CopyOnWriteArrayList<>();
        this.beans = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();

        this.beanResolver = new BeanResolver(this);
        this.interceptorResolver = new InterceptorResolver(this);
        this.removeFinalForProxyableMethods = removeFinalForProxyableMethods;
    }

    ContextRegistrar.RegistrationContext registerCustomContexts(List<ContextRegistrar> contextRegistrars) {
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
        if (buildContext != null) {
            List<ScopeInfo> allScopes = Arrays.stream(BuiltinScope.values()).map(i -> i.getInfo()).collect(Collectors.toList());
            allScopes.addAll(customContexts.keySet());
            buildContext.putInternal(Key.SCOPES.asString(), Collections.unmodifiableList(allScopes));
        }
        return registrationContext;
    }

    BeanRegistrar.RegistrationContext registerBeans(List<BeanRegistrar> beanRegistrars) {
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        this.beans.addAll(findBeans(initBeanDefiningAnnotations(beanDefiningAnnotations, stereotypes.keySet()), observers,
                injectionPoints));
        buildContextPut(Key.BEANS.asString(), Collections.unmodifiableList(beans));
        buildContextPut(Key.OBSERVERS.asString(), Collections.unmodifiableList(observers));

        this.interceptors.addAll(findInterceptors(injectionPoints));
        this.injectionPoints.addAll(injectionPoints);
        buildContextPut(Key.INJECTION_POINTS.asString(), Collections.unmodifiableList(this.injectionPoints));

        return registerSyntheticBeans(beanRegistrars, buildContext);
    }

    void init(Consumer<BytecodeTransformer> bytecodeTransformerConsumer) {
        long start = System.currentTimeMillis();

        // Collect dependency resolution errors
        List<Throwable> errors = new ArrayList<>();
        for (BeanInfo bean : beans) {
            bean.init(errors, bytecodeTransformerConsumer, removeFinalForProxyableMethods);
        }
        for (ObserverInfo observer : observers) {
            observer.init(errors);
        }
        for (InterceptorInfo interceptor : interceptors) {
            interceptor.init(errors, bytecodeTransformerConsumer, removeFinalForProxyableMethods);
        }
        processErrors(errors);

        if (removeUnusedBeans) {
            long removalStart = System.currentTimeMillis();
            Set<BeanInfo> removable = new HashSet<>();
            Set<BeanInfo> unusedProducers = new HashSet<>();
            Set<BeanInfo> unusedButDeclaresProducer = new HashSet<>();
            List<BeanInfo> producers = beans.stream().filter(b -> b.isProducerMethod() || b.isProducerField())
                    .collect(Collectors.toList());
            List<InjectionPointInfo> instanceInjectionPoints = injectionPoints.stream()
                    .filter(BuiltinBean.INSTANCE::matches)
                    .collect(Collectors.toList());
            Set<BeanInfo> injected = injectionPoints.stream().map(InjectionPointInfo::getResolvedBean)
                    .collect(Collectors.toSet());
            Set<BeanInfo> declaresProducer = producers.stream().map(BeanInfo::getDeclaringBean).collect(Collectors.toSet());
            Set<BeanInfo> declaresObserver = observers.stream().map(ObserverInfo::getDeclaringBean).collect(Collectors.toSet());
            test: for (BeanInfo bean : beans) {
                // Named beans can be used in templates and expressions
                if (bean.getName() != null) {
                    continue test;
                }
                // Custom exclusions
                for (Predicate<BeanInfo> exclusion : unusedExclusions) {
                    if (exclusion.test(bean)) {
                        continue test;
                    }
                }
                // Is injected
                if (injected.contains(bean)) {
                    continue test;
                }
                // Declares an observer method
                if (declaresObserver.contains(bean)) {
                    continue test;
                }
                // Instance<Foo>
                for (InjectionPointInfo injectionPoint : instanceInjectionPoints) {
                    if (Beans.hasQualifiers(bean, injectionPoint.getRequiredQualifiers()) && Beans.matchesType(bean,
                            injectionPoint.getRequiredType().asParameterizedType().arguments().get(0))) {
                        continue test;
                    }
                }
                // Declares a producer - see also second pass
                if (declaresProducer.contains(bean)) {
                    unusedButDeclaresProducer.add(bean);
                    continue test;
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
                    BeanInfo declaringBean = entry.getKey();
                    if (unusedButDeclaresProducer.contains(declaringBean) && unusedProducers.containsAll(entry.getValue())) {
                        // All producers declared by this bean are unused
                        removable.add(declaringBean);
                    }
                }
            }
            if (!removable.isEmpty()) {
                beans.removeAll(removable);
                removedBeans.addAll(removable);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debugf(removedBeans.stream().map(b -> "Removed unused " + b).collect(Collectors.joining("\n")));
                }
            }
            LOGGER.debugf("Removed %s unused beans in %s ms", removable.size(), System.currentTimeMillis() - removalStart);
        }
        LOGGER.debugf("Bean deployment initialized in %s ms", System.currentTimeMillis() - start);
    }

    ValidationContext validate(List<BeanDeploymentValidator> validators) {
        // Validate the bean deployment
        List<Throwable> errors = new ArrayList<>();
        validateBeans(errors, validators);
        ValidationContextImpl validationContext = new ValidationContextImpl(buildContext);
        for (Throwable error : errors) {
            validationContext.addDeploymentProblem(error);
        }
        for (BeanDeploymentValidator validator : validators) {
            validator.validate(validationContext);
        }
        return validationContext;
    }

    public Collection<BeanInfo> getBeans() {
        return Collections.unmodifiableList(beans);
    }

    public Collection<BeanInfo> getRemovedBeans() {
        return Collections.unmodifiableSet(removedBeans);
    }

    public Collection<ClassInfo> getQualifiers() {
        return Collections.unmodifiableCollection(qualifiers.values());
    }

    public Collection<ObserverInfo> getObservers() {
        return observers;
    }

    public Collection<InterceptorInfo> getInterceptors() {
        return interceptors;
    }

    public IndexView getIndex() {
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

    Set<AnnotationInstance> getTransitiveInterceptorBindings(DotName name) {
        return transitiveInterceptorBindings.get(name);
    }

    Map<DotName, Set<AnnotationInstance>> getTransitiveInterceptorBindings() {
        return transitiveInterceptorBindings;
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

    private void buildContextPut(String key, Object value) {
        if (buildContext != null) {
            buildContext.putInternal(key, value);
        }
    }

    private static Map<DotName, ClassInfo> findQualifiers(IndexView index) {
        Map<DotName, ClassInfo> qualifiers = new HashMap<>();
        for (AnnotationInstance qualifier : index.getAnnotations(DotNames.QUALIFIER)) {
            qualifiers.put(qualifier.target().asClass().name(), qualifier.target().asClass());
        }
        return qualifiers;
    }

    private static Map<DotName, ClassInfo> findInterceptorBindings(IndexView index) {
        Map<DotName, ClassInfo> bindings = new HashMap<>();
        // Note: doesn't use AnnotationStore, this will operate on classes without applying annotation transformers
        for (AnnotationInstance binding : index.getAnnotations(DotNames.INTERCEPTOR_BINDING)) {
            bindings.put(binding.target().asClass().name(), binding.target().asClass());
        }
        return bindings;
    }

    private static Map<DotName, Set<AnnotationInstance>> findTransitiveInterceptorBindigs(Collection<DotName> initialBindings,
            IndexView index,
            Map<DotName, Set<AnnotationInstance>> result, Map<DotName, ClassInfo> interceptorBindings,
            AnnotationStore annotationStore) {
        // for all known interceptor bindings
        for (DotName annotationName : initialBindings) {
            Set<AnnotationInstance> transitiveBindings = new HashSet<>();
            // for all annotations on them; use AnnotationStore to have up-to-date info
            for (AnnotationInstance bindingCandidate : annotationStore
                    .getAnnotations(interceptorBindings.get(annotationName))) {
                // if the annotation is an interceptor binding itself
                // Note: this verifies it against bindings found without application of transformers
                if (interceptorBindings.get(bindingCandidate.name()) != null) {
                    // register as transitive binding
                    transitiveBindings.add(bindingCandidate);
                }
            }
            if (!transitiveBindings.isEmpty()) {
                result.computeIfAbsent(annotationName, k -> new HashSet<>()).addAll(transitiveBindings);
            }
        }
        // now iterate over all so we can put together list for arbitrary transitive depth
        for (DotName name : result.keySet()) {
            result.put(name, recursiveBuild(name, result));
        }
        return result;
    }

    private static Set<AnnotationInstance> recursiveBuild(DotName name,
            Map<DotName, Set<AnnotationInstance>> transitiveBindingsMap) {
        Set<AnnotationInstance> result = transitiveBindingsMap.get(name);
        for (AnnotationInstance instance : transitiveBindingsMap.get(name)) {
            if (transitiveBindingsMap.containsKey(instance.name())) {
                // recursively find
                result.addAll(recursiveBuild(instance.name(), transitiveBindingsMap));
            }
        }
        return result;
    }

    private static Map<DotName, StereotypeInfo> findStereotypes(IndexView index, Map<DotName, ClassInfo> interceptorBindings,
            Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations,
            Map<ScopeInfo, Function<MethodCreator, ResultHandle>> customContexts,
            Map<DotName, Collection<AnnotationInstance>> additionalStereotypes, AnnotationStore annotationStore) {

        Map<DotName, StereotypeInfo> stereotypes = new HashMap<>();
        final List<AnnotationInstance> stereotypeAnnotations = new ArrayList<>(index.getAnnotations(DotNames.STEREOTYPE));
        for (final Collection<AnnotationInstance> annotations : additionalStereotypes.values()) {
            stereotypeAnnotations.addAll(annotations);
        }
        for (AnnotationInstance stereotype : stereotypeAnnotations) {
            final DotName stereotypeName = stereotype.target().asClass().name();
            ClassInfo stereotypeClass = getClassByName(index, stereotypeName);
            if (stereotypeClass != null) {

                boolean isAlternative = false;
                Integer alternativePriority = null;
                List<ScopeInfo> scopes = new ArrayList<>();
                List<AnnotationInstance> bindings = new ArrayList<>();
                boolean isNamed = false;

                for (AnnotationInstance annotation : annotationStore.getAnnotations(stereotypeClass)) {
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
                    } else if (DotNames.PRIORITY.equals(annotation.name())) {
                        alternativePriority = annotation.value().asInt();
                    } else {
                        final ScopeInfo scope = getScope(annotation.name(), customContexts);
                        if (scope != null) {
                            scopes.add(scope);
                        }
                    }
                }
                final ScopeInfo scope = getValidScope(scopes, stereotypeClass);
                stereotypes.put(stereotypeName, new StereotypeInfo(scope, bindings, isAlternative, alternativePriority,
                        isNamed, stereotypeClass));
            }
        }
        //if an additional bean defining annotation has a default scope we register it as a stereotype
        if (additionalBeanDefiningAnnotations != null) {
            for (BeanDefiningAnnotation i : additionalBeanDefiningAnnotations) {
                if (i.getDefaultScope() != null) {
                    ScopeInfo scope = getScope(i.getDefaultScope(), customContexts);
                    ClassInfo stereotypeClassInfo = getClassByName(index, i.getAnnotation());
                    if (stereotypeClassInfo != null) {
                        stereotypes.put(i.getAnnotation(), new StereotypeInfo(scope, Collections.emptyList(),
                                false, null, false, true,
                                stereotypeClassInfo));
                    }
                }
            }
        }

        return stereotypes;
    }

    private static ScopeInfo getScope(DotName scopeAnnotationName,
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

    private List<BeanInfo> findBeans(Collection<DotName> beanDefiningAnnotations, List<ObserverInfo> observers,
            List<InjectionPointInfo> injectionPoints) {

        Set<ClassInfo> beanClasses = new HashSet<>();
        Set<MethodInfo> producerMethods = new HashSet<>();
        Set<MethodInfo> disposerMethods = new HashSet<>();
        Set<FieldInfo> producerFields = new HashSet<>();
        Map<MethodInfo, Set<ClassInfo>> syncObserverMethods = new HashMap<>();
        Map<MethodInfo, Set<ClassInfo>> asyncObserverMethods = new HashMap<>();

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

            // non-inherited stuff:
            for (MethodInfo method : beanClass.methods()) {
                if (Methods.isSynthetic(method)) {
                    continue;
                }
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
                }
            }

            // inherited stuff
            ClassInfo aClass = beanClass;
            Set<ClassInfo> scannedClasses = new HashSet<>();
            Set<MethodInfo> methods = new HashSet<>();
            while (aClass != null) {
                if (!scannedClasses.add(aClass)) {
                    continue;
                }
                for (MethodInfo method : aClass.methods()) {
                    if (Methods.isSynthetic(method) || Methods.isOverriden(method, methods)) {
                        continue;
                    }
                    methods.add(method);
                    if (annotationStore.getAnnotations(method).isEmpty()) {
                        continue;
                    }
                    if (annotationStore.hasAnnotation(method, DotNames.OBSERVES)) {
                        syncObserverMethods.computeIfAbsent(method, ignored -> new HashSet<>())
                                .add(beanClass);
                        if (!Modifier.isAbstract(beanClass.flags())) {
                            // add only concrete classes
                            beanClasses.add(beanClass);
                            if (!hasBeanDefiningAnnotation) {
                                LOGGER.debugf("Observer method found but %s has no bean defining annotation - using @Dependent",
                                        beanClass);
                            }
                        }
                    } else if (annotationStore.hasAnnotation(method, DotNames.OBSERVES_ASYNC)) {
                        asyncObserverMethods.computeIfAbsent(method, ignored -> new HashSet<>())
                                .add(beanClass);
                        if (!Modifier.isAbstract(beanClass.flags())) {
                            // add only concrete classes
                            beanClasses.add(beanClass);
                            if (!hasBeanDefiningAnnotation) {
                                LOGGER.debugf("Observer method found but %s has no bean defining annotation - using @Dependent",
                                        beanClass);
                            }
                        }
                    }
                }
                Type superType = aClass.superClassType();
                aClass = superType != null && !superType.name().equals(DotNames.OBJECT)
                        && CLASS_TYPES.contains(superType.kind())
                                ? getClassByName(index, superType.name())
                                : null;
            }
            for (FieldInfo field : beanClass.fields()) {
                if (annotationStore.hasAnnotation(field, DotNames.PRODUCES)) {
                    if (annotationStore.hasAnnotation(field, DotNames.INJECT)) {
                        throw new DefinitionException("Injected field cannot be annotated with @Produces: " + field);
                    }
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
            BeanInfo classBean = Beans.createClassBean(beanClass, this, injectionPointTransformer);
            beans.add(classBean);
            beanClassToBean.put(beanClass, classBean);
            injectionPoints.addAll(classBean.getAllInjectionPoints());
        }

        List<DisposerInfo> disposers = new ArrayList<>();
        for (MethodInfo disposerMethod : disposerMethods) {
            BeanInfo declaringBean = beanClassToBean.get(disposerMethod.declaringClass());
            if (declaringBean != null) {
                Injection injection = Injection.forDisposer(disposerMethod, declaringBean.getImplClazz(), this,
                        injectionPointTransformer);
                disposers.add(new DisposerInfo(declaringBean, disposerMethod, injection));
                injectionPoints.addAll(injection.injectionPoints);
            }
        }

        for (MethodInfo producerMethod : producerMethods) {
            BeanInfo declaringBean = beanClassToBean.get(producerMethod.declaringClass());
            if (declaringBean != null) {
                BeanInfo producerMethodBean = Beans.createProducerMethod(producerMethod, declaringBean, this,
                        findDisposer(declaringBean, producerMethod, disposers), injectionPointTransformer);
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

        for (Map.Entry<MethodInfo, Set<ClassInfo>> syncObserverEntry : syncObserverMethods.entrySet()) {
            registerObserverMethods(syncObserverEntry.getValue(), observers, injectionPoints,
                    beanClassToBean, syncObserverEntry.getKey(), false);
        }

        for (Map.Entry<MethodInfo, Set<ClassInfo>> syncObserverEntry : asyncObserverMethods.entrySet()) {
            registerObserverMethods(syncObserverEntry.getValue(), observers, injectionPoints,
                    beanClassToBean, syncObserverEntry.getKey(), true);
        }

        if (LOGGER.isTraceEnabled()) {
            for (BeanInfo bean : beans) {
                LOGGER.logf(Level.TRACE, "Created %s", bean);
            }
        }
        return beans;
    }

    private void registerObserverMethods(Collection<ClassInfo> classes,
            List<ObserverInfo> observers,
            List<InjectionPointInfo> injectionPoints,
            Map<ClassInfo, BeanInfo> beanClassToBean,
            MethodInfo observerMethod,
            boolean async) {
        for (ClassInfo key : classes) {
            BeanInfo declaringBean = beanClassToBean.get(key);
            if (declaringBean != null) {
                Injection injection = Injection.forObserver(observerMethod, declaringBean.getImplClazz(), this,
                        injectionPointTransformer);
                observers.add(new ObserverInfo(declaringBean, observerMethod, injection, async));
                injectionPoints.addAll(injection.injectionPoints);
            }
        }
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
                            disposer.getDisposedParameterQualifiers())) {
                        hasQualifier = false;
                    }
                }
                if (hasQualifier && beanResolver.matches(beanType, disposer.getDisposedParameterType())) {
                    found.add(disposer);
                }
            }
        }
        if (found.size() > 1) {
            throw new DefinitionException("Multiple disposer methods found for " + annotationTarget);
        }
        return found.isEmpty() ? null : found.get(0);
    }

    // keep it public we need this method in quarkus integration
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

    private RegistrationContext registerSyntheticBeans(List<BeanRegistrar> beanRegistrars, BuildContext buildContext) {
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
        return registrationContext;
    }

    static void processErrors(List<Throwable> errors) {
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

    private List<InterceptorInfo> findInterceptors(List<InjectionPointInfo> injectionPoints) {
        Set<ClassInfo> interceptorClasses = new HashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(DotNames.INTERCEPTOR)) {
            if (Kind.CLASS.equals(annotation.target().kind())) {
                interceptorClasses.add(annotation.target().asClass());
            }
        }
        List<InterceptorInfo> interceptors = new ArrayList<>();
        for (ClassInfo interceptorClass : interceptorClasses) {
            interceptors
                    .add(Interceptors.createInterceptor(interceptorClass, this, injectionPointTransformer, annotationStore));
        }
        if (LOGGER.isTraceEnabled()) {
            for (InterceptorInfo interceptor : interceptors) {
                LOGGER.logf(Level.TRACE, "Created %s", interceptor);
            }
        }
        for (InterceptorInfo interceptor : interceptors) {
            injectionPoints.addAll(interceptor.getAllInjectionPoints());
        }
        return interceptors;
    }

    private void validateBeans(List<Throwable> errors, List<BeanDeploymentValidator> validators) {
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
            bean.validate(errors, validators);
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

    public Set<String> getNonBindingFields(DotName name) {
        return nonBindingFields.getOrDefault(name, Collections.emptySet());
    }

    private static class ValidationContextImpl implements ValidationContext {

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

        @Override
        public List<Throwable> getDeploymentProblems() {
            return Collections.unmodifiableList(errors);
        }

    }

}

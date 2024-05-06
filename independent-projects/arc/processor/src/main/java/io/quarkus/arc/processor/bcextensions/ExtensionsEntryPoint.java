package io.quarkus.arc.processor.bcextensions;

import static io.quarkus.arc.processor.Beans.stereotypesWithTransitive;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanDisposer;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserver;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.util.Nonbinding;

import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MutableAnnotationOverlay;

import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.InstanceImpl;
import io.quarkus.arc.impl.SyntheticCreationalContextImpl;
import io.quarkus.arc.impl.bcextensions.ParametersImpl;
import io.quarkus.arc.processor.BeanArchives;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.ConfiguratorBase;
import io.quarkus.arc.processor.ContextConfigurator;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.arc.processor.CustomAlterableContexts;
import io.quarkus.arc.processor.CustomAlterableContexts.CustomAlterableContextInfo;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.arc.processor.ObserverConfigurator;
import io.quarkus.arc.processor.ObserverInfo;
import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.arc.processor.ScopeInfo;
import io.quarkus.arc.processor.StereotypeInfo;
import io.quarkus.arc.processor.StereotypeRegistrar;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 * Entrypoint for the Build Compatible Extensions implementation. Used by the rest of ArC.
 * <p>
 * Exactly one instance of this class is supposed to exist for a single {@code BeanProcessor}.
 * <p>
 * All methods are single-threaded (serial), but it is expected that one may be called
 * on a different thread than the other. Therefore, this class as well as all other classes
 * that implement the BCExtensions API do not guard data against concurrent access, but they
 * do ensure visibility.
 */
public class ExtensionsEntryPoint {
    private final ExtensionInvoker invoker;
    private final SharedErrors errors;

    private final Map<DotName, ClassConfig> qualifiers;
    private final Map<DotName, ClassConfig> interceptorBindings;
    private final Map<DotName, ClassConfig> stereotypes;
    private final List<MetaAnnotationsImpl.ContextData> contexts;
    private final List<AnnotationTransformation> preAnnotationTransformations;

    private volatile MutableAnnotationOverlay annotationOverlay;

    private final List<SyntheticBeanBuilderImpl<?>> syntheticBeans;
    private final List<SyntheticObserverBuilderImpl<?>> syntheticObservers;

    public ExtensionsEntryPoint() {
        this(List.of());
    }

    // only for `ArcTestContainer`
    public ExtensionsEntryPoint(List<BuildCompatibleExtension> extensions) {
        invoker = new ExtensionInvoker(extensions);
        if (invoker.isEmpty()) {
            errors = null;
            qualifiers = null;
            interceptorBindings = null;
            stereotypes = null;
            contexts = null;
            preAnnotationTransformations = null;
            syntheticBeans = null;
            syntheticObservers = null;
        } else {
            errors = new SharedErrors();
            qualifiers = new ConcurrentHashMap<>();
            interceptorBindings = new ConcurrentHashMap<>();
            stereotypes = new ConcurrentHashMap<>();
            contexts = Collections.synchronizedList(new ArrayList<>());
            preAnnotationTransformations = Collections.synchronizedList(new ArrayList<>());
            syntheticBeans = Collections.synchronizedList(new ArrayList<>());
            syntheticObservers = Collections.synchronizedList(new ArrayList<>());
        }
    }

    /**
     * Must be called first, <i>before</i> {@code registerMetaAnnotations}.
     * <p>
     * It is a no-op if no {@link BuildCompatibleExtension} was found.
     */
    public void runDiscovery(org.jboss.jandex.IndexView applicationIndex, Set<String> additionalClasses) {
        if (invoker.isEmpty()) {
            return;
        }

        MutableAnnotationOverlay overlay = MutableAnnotationOverlay.builder(applicationIndex)
                .compatibleMode()
                .runtimeAnnotationsOnly()
                .inheritedAnnotations()
                .build();
        try {
            BuildServicesImpl.init(applicationIndex, overlay);

            // the method name is `buildComputingBeanArchiveIndex`, but it just builds a computing index
            // on top of whatever index is passed to it
            org.jboss.jandex.IndexView computingApplicationIndex = BeanArchives.buildComputingBeanArchiveIndex(
                    Thread.currentThread().getContextClassLoader(), new ConcurrentHashMap<>(), applicationIndex);

            new ExtensionPhaseDiscovery(invoker, computingApplicationIndex, errors, additionalClasses,
                    overlay, qualifiers, interceptorBindings, stereotypes, contexts).run();
        } finally {
            // noone should attempt annotation transformations on custom meta-annotations after `@Discovery` is finished
            preAnnotationTransformations.addAll(overlay.freeze());

            BuildServicesImpl.reset();
        }
    }

    /**
     * Must be called <i>after</i> {@code runDiscovery} and <i>before</i> {@code runEnhancement}.
     * <p>
     * It is a no-op if no {@link BuildCompatibleExtension} was found.
     */
    public void registerMetaAnnotations(BeanProcessor.Builder builder, CustomAlterableContexts customAlterableContexts) {
        if (invoker.isEmpty()) {
            return;
        }

        builder.addAnnotationTransformation(new AnnotationTransformation() {
            @Override
            public void apply(TransformationContext context) {
                for (AnnotationTransformation preAnnotationTransformation : preAnnotationTransformations) {
                    if (preAnnotationTransformation.supports(context.declaration().kind())) {
                        preAnnotationTransformation.apply(context);
                    }
                }
            }
        });

        if (!qualifiers.isEmpty()) {
            builder.addQualifierRegistrar(new QualifierRegistrar() {
                @Override
                public Map<DotName, Set<String>> getAdditionalQualifiers() {
                    Map<DotName, Set<String>> result = new HashMap<>();
                    for (Map.Entry<DotName, ClassConfig> entry : qualifiers.entrySet()) {
                        DotName annotationName = entry.getKey();
                        ClassConfig config = entry.getValue();

                        Set<String> nonbindingMembers = config.methods()
                                .stream()
                                .filter(it -> it.info().hasAnnotation(Nonbinding.class))
                                .map(it -> it.info().name())
                                .collect(Collectors.toUnmodifiableSet());
                        result.put(annotationName, nonbindingMembers);
                    }
                    return result;
                }
            });
        }

        if (!interceptorBindings.isEmpty()) {
            builder.addInterceptorBindingRegistrar(new InterceptorBindingRegistrar() {
                @Override
                public List<InterceptorBinding> getAdditionalBindings() {
                    return interceptorBindings.entrySet()
                            .stream()
                            .map(entry -> {
                                DotName annotationName = entry.getKey();
                                ClassConfig config = entry.getValue();

                                Set<String> nonbindingMembers = config.methods()
                                        .stream()
                                        .filter(it -> it.info().hasAnnotation(Nonbinding.class))
                                        .map(it -> it.info().name())
                                        .collect(Collectors.toUnmodifiableSet());

                                return InterceptorBinding.of(annotationName, nonbindingMembers);
                            })
                            .toList();
                }
            });
        }

        if (!stereotypes.isEmpty()) {
            builder.addStereotypeRegistrar(new StereotypeRegistrar() {
                @Override
                public Set<DotName> getAdditionalStereotypes() {
                    return stereotypes.keySet();
                }
            });
        }

        if (!contexts.isEmpty()) {
            for (MetaAnnotationsImpl.ContextData context : contexts) {
                builder.addContextRegistrar(new ContextRegistrar() {
                    @Override
                    public void register(RegistrationContext registrationContext) {
                        Class<? extends Annotation> scopeAnnotation = context.scopeAnnotation;
                        Class<? extends AlterableContext> contextClass = context.contextClass;

                        ContextConfigurator config = registrationContext.configure(scopeAnnotation);
                        if (InjectableContext.class.isAssignableFrom(contextClass)) {
                            config.contextClass((Class<? extends InjectableContext>) contextClass);
                        } else {
                            CustomAlterableContextInfo info = customAlterableContexts.add(contextClass, context.isNormal,
                                    scopeAnnotation);
                            config.creator(bytecode -> {
                                return bytecode.newInstance(MethodDescriptor.ofConstructor(info.generatedName));
                            });
                        }
                        if (context.isNormal != null) {
                            config.normal(context.isNormal);
                        }
                        config.done();
                    }
                });
            }
        }
    }

    /**
     * Must be called <i>after</i> {@code registerMetaAnnotations} and <i>before</i> {@code runRegistration}.
     * <p>
     * It is a no-op if no {@link BuildCompatibleExtension} was found.
     */
    public void runEnhancement(org.jboss.jandex.IndexView beanArchiveIndex, BeanProcessor.Builder builder) {
        if (invoker.isEmpty()) {
            return;
        }

        annotationOverlay = MutableAnnotationOverlay.builder(beanArchiveIndex)
                .compatibleMode()
                .runtimeAnnotationsOnly()
                .inheritedAnnotations()
                .build();

        BuildServicesImpl.init(beanArchiveIndex, annotationOverlay);

        try {
            new ExtensionPhaseEnhancement(invoker, beanArchiveIndex, errors, annotationOverlay).run();
        } finally {
            // noone should attempt annotation transformations on application classes after `@Enhancement` is finished
            List<AnnotationTransformation> annotationTransformations = annotationOverlay.freeze();

            builder.addAnnotationTransformation(new AnnotationTransformation() {
                @Override
                public void apply(TransformationContext context) {
                    for (AnnotationTransformation annotationTransformation : annotationTransformations) {
                        if (annotationTransformation.supports(context.declaration().kind())) {
                            annotationTransformation.apply(context);
                        }
                    }
                }
            });

            BuildServicesImpl.reset();
        }
    }

    /**
     * Must be called <i>after</i> {@code runEnhancement} and <i>before</i> {@code runSynthesis}.
     * <p>
     * It is a no-op if no {@link BuildCompatibleExtension} was found.
     */
    public void runRegistration(org.jboss.jandex.IndexView beanArchiveIndex,
            Collection<io.quarkus.arc.processor.BeanInfo> allBeans,
            Collection<io.quarkus.arc.processor.InterceptorInfo> allInterceptors,
            Collection<io.quarkus.arc.processor.ObserverInfo> allObservers,
            io.quarkus.arc.processor.InvokerFactory invokerFactory) {
        if (invoker.isEmpty()) {
            return;
        }

        BuildServicesImpl.init(beanArchiveIndex, annotationOverlay);

        try {
            new ExtensionPhaseRegistration(invoker, beanArchiveIndex, errors, annotationOverlay,
                    allBeans, allInterceptors, allObservers, invokerFactory).run();
        } finally {
            BuildServicesImpl.reset();
        }
    }

    /**
     * Must be called <i>after</i> {@code runRegistration} and <i>before</i> {@code registerSyntheticBeans}.
     * <p>
     * It is a no-op if no {@link BuildCompatibleExtension} was found.
     */
    public void runSynthesis(org.jboss.jandex.IndexView beanArchiveIndex) {
        if (invoker.isEmpty()) {
            return;
        }

        BuildServicesImpl.init(beanArchiveIndex, annotationOverlay);

        try {
            new ExtensionPhaseSynthesis(invoker, beanArchiveIndex, errors, annotationOverlay,
                    syntheticBeans, syntheticObservers).run();
        } finally {
            BuildServicesImpl.reset();
        }
    }

    /**
     * Must be called <i>after</i> {@code runSynthesis} and <i>before</i> {@code runRegistrationAgain}.
     * <p>
     * It is a no-op if no {@link BuildCompatibleExtension} was found.
     */
    public void registerSyntheticBeans(BeanRegistrar.RegistrationContext context, Predicate<DotName> isApplicationClass) {
        if (invoker.isEmpty()) {
            return;
        }

        Map<DotName, StereotypeInfo> allStereotypes = context.get(BuildExtension.Key.STEREOTYPES);

        for (SyntheticBeanBuilderImpl<?> syntheticBean : syntheticBeans) {
            StereotypeInfo[] stereotypes = syntheticBean.stereotypes.stream()
                    .map(allStereotypes::get)
                    .toArray(StereotypeInfo[]::new);

            // no need to verify if scope inheritance from stereotypes is valid, that will be checked later
            // this means that `dependentByStereotype` may be inaccurate, but deployment will eventually fail if so
            boolean dependentByStereotype = false;
            for (StereotypeInfo stereotype : stereotypesWithTransitive(Arrays.asList(stereotypes), allStereotypes)) {
                ScopeInfo defaultScope = stereotype.getDefaultScope();
                if (defaultScope != null) {
                    if (BuiltinScope.DEPENDENT.is(defaultScope)) {
                        dependentByStereotype = true;
                        break;
                    }
                }
            }

            BeanConfigurator<Object> bean = context.configure(syntheticBean.implementationClass)
                    .types(syntheticBean.types.toArray(new org.jboss.jandex.Type[0]))
                    .qualifiers(syntheticBean.qualifiers.toArray(new org.jboss.jandex.AnnotationInstance[0]))
                    .stereotypes(stereotypes);
            if (syntheticBean.scope != null) {
                bean.scope(syntheticBean.scope);
            }
            if (syntheticBean.name != null) {
                bean.name(syntheticBean.name);
            }
            if (syntheticBean.isAlternative) {
                bean.alternative(true);
            }
            if (syntheticBean.priority != null) {
                bean.priority(syntheticBean.priority);
            }
            configureParams(bean, syntheticBean.params);
            boolean isDependent = syntheticBean.scope == null
                    || Dependent.class.equals(syntheticBean.scope)
                    || dependentByStereotype;
            bean.creator(mc -> { // generated method signature: Object(SyntheticCreationalContext)
                // | SyntheticCreationalContextImpl synthCC = (SyntheticCreationalContextImpl) creationalContext;
                ResultHandle synthCC = mc.checkCast(mc.getMethodParam(0), SyntheticCreationalContextImpl.class);
                // | CreationalContext delegateCC = synthCC.getDelegateCreationalContext()
                ResultHandle delegateCC = mc.invokeVirtualMethod(MethodDescriptor.ofMethod(SyntheticCreationalContextImpl.class,
                        "getDelegateCreationalContext", CreationalContext.class), synthCC);
                // | CreationalContextImpl ccImpl = (CreationalContextImpl) delegateCC;
                ResultHandle ccImpl = mc.checkCast(delegateCC, CreationalContextImpl.class);
                // | Instance<Object> lookup = InstanceImpl.forSynthesis(creationalContext, isDependent);
                ResultHandle lookup = mc.invokeStaticMethod(MethodDescriptor.ofMethod(InstanceImpl.class,
                        "forSynthesis", Instance.class, CreationalContextImpl.class, boolean.class),
                        ccImpl, mc.load(isDependent));

                // | Map<String, Object> paramsMap = this.params;
                // the generated bean class has a "params" field filled with all the data
                ResultHandle paramsMap = mc.readInstanceField(
                        FieldDescriptor.of(mc.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                        mc.getThis());
                // | Parameters params = new ParametersImpl(paramsMap);
                ResultHandle params = mc.newInstance(MethodDescriptor.ofConstructor(ParametersImpl.class, Map.class),
                        paramsMap);

                // | SyntheticBeanCreator creator = new ConfiguredSyntheticBeanCreator();
                ResultHandle creator = mc.newInstance(MethodDescriptor.ofConstructor(syntheticBean.creatorClass));

                // | Object instance = creator.create(lookup, params);
                ResultHandle[] args = { lookup, params };
                ResultHandle instance = mc.invokeInterfaceMethod(MethodDescriptor.ofMethod(SyntheticBeanCreator.class,
                        "create", Object.class, Instance.class, Parameters.class), creator, args);

                // | return instance;
                mc.returnValue(instance);
            });
            if (syntheticBean.disposerClass != null) {
                bean.destroyer(mc -> { // generated method signature: void(Object, CreationalContext)
                    // | CreationalContextImpl creationalContextImpl = (CreationalContextImpl) creationalContext;
                    ResultHandle creationalContextImpl = mc.checkCast(mc.getMethodParam(1), CreationalContextImpl.class);
                    // | Instance<Object> lookup = InstanceImpl.forSynthesis(creationalContext, isDependent);
                    ResultHandle lookup = mc.invokeStaticMethod(MethodDescriptor.ofMethod(InstanceImpl.class,
                            "forSynthesis", Instance.class, CreationalContextImpl.class, boolean.class),
                            creationalContextImpl, mc.load(false)); // looking up InjectionPoint in disposer is invalid

                    // | Map<String, Object> paramsMap = this.params;
                    // the generated bean class has a "params" field filled with all the data
                    ResultHandle paramsMap = mc.readInstanceField(
                            FieldDescriptor.of(mc.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                            mc.getThis());
                    // | Parameters params = new ParametersImpl(paramsMap);
                    ResultHandle params = mc.newInstance(MethodDescriptor.ofConstructor(ParametersImpl.class, Map.class),
                            paramsMap);

                    // | SyntheticBeanDisposer disposer = new ConfiguredSyntheticBeanDisposer();
                    ResultHandle disposer = mc.newInstance(MethodDescriptor.ofConstructor(syntheticBean.disposerClass));

                    // | disposer.dispose(instance, lookup, params);
                    ResultHandle[] args = { mc.getMethodParam(0), lookup, params };
                    mc.invokeInterfaceMethod(MethodDescriptor.ofMethod(SyntheticBeanDisposer.class, "dispose",
                            void.class, Object.class, Instance.class, Parameters.class), disposer, args);

                    // | creationalContextImpl.release()
                    mc.invokeVirtualMethod(MethodDescriptor.ofMethod(CreationalContextImpl.class, "release", void.class),
                            creationalContextImpl);

                    // return type is void
                    mc.returnValue(null);
                });
            }
            // the generated classes need to see the `creatorClass` and the `disposerClass`,
            // so if they are application classes, the generated classes are forced to also
            // be application classes, even if the `implementationClass` possibly isn't
            if (isApplicationClass.test(DotName.createSimple(syntheticBean.creatorClass))) {
                bean.forceApplicationClass();
            }
            if (syntheticBean.disposerClass != null
                    && isApplicationClass.test(DotName.createSimple(syntheticBean.disposerClass))) {
                bean.forceApplicationClass();
            }
            bean.done();
        }
    }

    /**
     * Must be called <i>after</i> {@code runSynthesis} and <i>before</i> {@code runRegistrationAgain}.
     * <p>
     * It is a no-op if no {@link BuildCompatibleExtension} was found.
     */
    public void registerSyntheticObservers(ObserverRegistrar.RegistrationContext context,
            Predicate<DotName> isApplicationClass) {
        if (invoker.isEmpty()) {
            return;
        }

        for (SyntheticObserverBuilderImpl<?> syntheticObserver : syntheticObservers) {
            if (syntheticObserver.isAsync && syntheticObserver.transactionPhase != TransactionPhase.IN_PROGRESS) {
                throw new IllegalStateException("Synthetic observer declared as asynchronous and transactional "
                        + "(event type " + syntheticObserver.type + ", \"declared\" by " + syntheticObserver.declaringClass
                        + ", notified using " + syntheticObserver.implementationClass + ")");
            }

            ObserverConfigurator observer = context.configure()
                    .beanClass(syntheticObserver.declaringClass)
                    .observedType(syntheticObserver.type)
                    .qualifiers(syntheticObserver.qualifiers.toArray(new org.jboss.jandex.AnnotationInstance[0]))
                    .priority(syntheticObserver.priority)
                    .async(syntheticObserver.isAsync)
                    .transactionPhase(syntheticObserver.transactionPhase);
            configureParams(observer, syntheticObserver.params);
            observer.notify(mc -> { // generated method signature: void(EventContext)
                // | SyntheticObserver instance = new ConfiguredEventConsumer();
                ResultHandle instance = mc.newInstance(MethodDescriptor.ofConstructor(syntheticObserver.implementationClass));

                // | Map<String, Object> paramsMap = this.params;
                // the generated observer class has a "params" field filled with all the data
                ResultHandle paramsMap = mc.readInstanceField(
                        FieldDescriptor.of(mc.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                        mc.getThis());

                // | Parameters params = new ParametersImpl(paramsMap);
                ResultHandle params = mc.newInstance(MethodDescriptor.ofConstructor(ParametersImpl.class, Map.class),
                        paramsMap);

                // | instance.observe(eventContext, params);
                ResultHandle[] args = { mc.getMethodParam(0), params };
                mc.invokeInterfaceMethod(MethodDescriptor.ofMethod(SyntheticObserver.class, "observe",
                        void.class, EventContext.class, Parameters.class), instance, args);

                // return type is void
                mc.returnValue(null);
            });
            // the generated classes need to see the `implementationClass`, so if it is
            // an application class, the generated classes are forced to also be application
            // classes, even if the `declaringClass` possibly isn't
            if (isApplicationClass.test(DotName.createSimple(syntheticObserver.implementationClass))) {
                observer.forceApplicationClass();
            }
            observer.done();
        }
    }

    private void configureParams(ConfiguratorBase<?> configurator, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                configurator.param(entry.getKey(), (Boolean) entry.getValue());
            } else if (entry.getValue() instanceof boolean[]) {
                configurator.param(entry.getKey(), (boolean[]) entry.getValue());
            } else if (entry.getValue() instanceof Byte) {
                configurator.param(entry.getKey(), (Byte) entry.getValue());
            } else if (entry.getValue() instanceof byte[]) {
                configurator.param(entry.getKey(), (byte[]) entry.getValue());
            } else if (entry.getValue() instanceof Short) {
                configurator.param(entry.getKey(), (Short) entry.getValue());
            } else if (entry.getValue() instanceof short[]) {
                configurator.param(entry.getKey(), (short[]) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                configurator.param(entry.getKey(), (Integer) entry.getValue());
            } else if (entry.getValue() instanceof int[]) {
                configurator.param(entry.getKey(), (int[]) entry.getValue());
            } else if (entry.getValue() instanceof Long) {
                configurator.param(entry.getKey(), (Long) entry.getValue());
            } else if (entry.getValue() instanceof long[]) {
                configurator.param(entry.getKey(), (long[]) entry.getValue());
            } else if (entry.getValue() instanceof Float) {
                configurator.param(entry.getKey(), (Float) entry.getValue());
            } else if (entry.getValue() instanceof float[]) {
                configurator.param(entry.getKey(), (float[]) entry.getValue());
            } else if (entry.getValue() instanceof Double) {
                configurator.param(entry.getKey(), (Double) entry.getValue());
            } else if (entry.getValue() instanceof double[]) {
                configurator.param(entry.getKey(), (double[]) entry.getValue());
            } else if (entry.getValue() instanceof Character) {
                configurator.param(entry.getKey(), (Character) entry.getValue());
            } else if (entry.getValue() instanceof char[]) {
                configurator.param(entry.getKey(), (char[]) entry.getValue());
            } else if (entry.getValue() instanceof String) {
                configurator.param(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof String[]) {
                configurator.param(entry.getKey(), (String[]) entry.getValue());
            } else if (entry.getValue() instanceof Enum<?>) {
                configurator.param(entry.getKey(), (Enum<?>) entry.getValue());
            } else if (entry.getValue() instanceof Enum<?>[]) {
                configurator.param(entry.getKey(), (Enum<?>[]) entry.getValue());
            } else if (entry.getValue() instanceof Class<?>) {
                configurator.param(entry.getKey(), (Class<?>) entry.getValue());
            } else if (entry.getValue() instanceof Class<?>[]) {
                configurator.param(entry.getKey(), (Class<?>[]) entry.getValue());
            } else if (entry.getValue() instanceof org.jboss.jandex.ClassInfo) {
                configurator.param(entry.getKey(), (org.jboss.jandex.ClassInfo) entry.getValue());
            } else if (entry.getValue() instanceof org.jboss.jandex.ClassInfo[]) {
                configurator.param(entry.getKey(), (org.jboss.jandex.ClassInfo[]) entry.getValue());
            } else if (entry.getValue() instanceof org.jboss.jandex.AnnotationInstance) {
                configurator.param(entry.getKey(), (org.jboss.jandex.AnnotationInstance) entry.getValue());
            } else if (entry.getValue() instanceof org.jboss.jandex.AnnotationInstance[]) {
                configurator.param(entry.getKey(), (org.jboss.jandex.AnnotationInstance[]) entry.getValue());
            } else if (entry.getValue() instanceof io.quarkus.arc.processor.InvokerInfo) {
                configurator.param(entry.getKey(), (io.quarkus.arc.processor.InvokerInfo) entry.getValue());
            } else if (entry.getValue() instanceof io.quarkus.arc.processor.InvokerInfo[]) {
                configurator.param(entry.getKey(), (io.quarkus.arc.processor.InvokerInfo[]) entry.getValue());
            } else {
                throw new IllegalStateException("Unknown param: " + entry);
            }
        }
    }

    /**
     * Must be called <i>after</i> {@code registerSynthetic{Beans,Observers}} and <i>before</i>
     * {@code runValidation}.
     * <p>
     * It is a no-op if no {@link BuildCompatibleExtension} was found.
     */
    public void runRegistrationAgain(org.jboss.jandex.IndexView beanArchiveIndex,
            Collection<io.quarkus.arc.processor.BeanInfo> allBeans,
            Collection<io.quarkus.arc.processor.ObserverInfo> allObservers,
            io.quarkus.arc.processor.InvokerFactory invokerFactory) {
        if (invoker.isEmpty()) {
            return;
        }

        Collection<io.quarkus.arc.processor.BeanInfo> syntheticBeans = allBeans.stream()
                .filter(BeanInfo::isSynthetic)
                .toList();
        Collection<io.quarkus.arc.processor.ObserverInfo> syntheticObservers = allObservers.stream()
                .filter(ObserverInfo::isSynthetic)
                .toList();

        BuildServicesImpl.init(beanArchiveIndex, annotationOverlay);

        try {
            new ExtensionPhaseRegistration(invoker, beanArchiveIndex, errors, annotationOverlay,
                    syntheticBeans, Collections.emptyList(), syntheticObservers, invokerFactory).run();
        } finally {
            BuildServicesImpl.reset();
        }
    }

    /**
     * Must be called <i>after</i> {@code runRegistrationAgain} and <i>before</i> {@code registerValidationErrors}.
     * <p>
     * It is a no-op if no {@link BuildCompatibleExtension} was found.
     */
    public void runValidation(org.jboss.jandex.IndexView beanArchiveIndex,
            Collection<io.quarkus.arc.processor.BeanInfo> allBeans,
            Collection<io.quarkus.arc.processor.ObserverInfo> allObservers) {
        if (invoker.isEmpty()) {
            return;
        }

        BuildServicesImpl.init(beanArchiveIndex, annotationOverlay);

        try {
            new ExtensionPhaseValidation(invoker, beanArchiveIndex, errors, annotationOverlay,
                    allBeans, allObservers).run();
        } finally {
            BuildServicesImpl.reset();
        }
    }

    /**
     * Must be called last, <i>after</i> {@code runValidation}.
     * <p>
     * It is a no-op if no {@link BuildCompatibleExtension} was found.
     */
    public void registerValidationErrors(BeanDeploymentValidator.ValidationContext context) {
        if (invoker.isEmpty()) {
            return;
        }

        for (Throwable error : errors.list) {
            context.addDeploymentProblem(error);
        }

        invoker.invalidate();
    }
}

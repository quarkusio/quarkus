package io.quarkus.scheduler.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;
import static org.jboss.jandex.AnnotationValue.createArrayValue;
import static org.jboss.jandex.AnnotationValue.createBooleanValue;
import static org.jboss.jandex.AnnotationValue.createStringValue;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.common.runtime.DefaultInvoker;
import io.quarkus.scheduler.common.runtime.MutableScheduledMethod;
import io.quarkus.scheduler.common.runtime.SchedulerContext;
import io.quarkus.scheduler.common.runtime.util.SchedulerUtils;
import io.quarkus.scheduler.runtime.CompositeScheduler;
import io.quarkus.scheduler.runtime.Constituent;
import io.quarkus.scheduler.runtime.SchedulerConfig;
import io.quarkus.scheduler.runtime.SchedulerRecorder;
import io.quarkus.scheduler.runtime.SimpleScheduler;
import io.smallrye.common.annotation.Identifier;

public class SchedulerProcessor {

    private static final Logger LOGGER = Logger.getLogger(SchedulerProcessor.class);

    static final Type SCHEDULED_EXECUTION_TYPE = Type.create(DotName.createSimple(ScheduledExecution.class.getName()),
            Kind.CLASS);

    static final String INVOKER_SUFFIX = "_ScheduledInvoker";
    static final String NESTED_SEPARATOR = "$_";

    @BuildStep
    SchedulerImplementationBuildItem implementation() {
        return new SchedulerImplementationBuildItem(Scheduled.SIMPLE, DotName.createSimple(SimpleScheduler.class), 0);
    }

    @BuildStep
    void compositeScheduler(SchedulerConfig config, List<SchedulerImplementationBuildItem> implementations,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<DiscoveredImplementationsBuildItem> discoveredImplementations) {
        List<SchedulerImplementationBuildItem> sorted = implementations.stream()
                .sorted(Comparator.comparingInt(SchedulerImplementationBuildItem::getPriority).reversed()).toList();
        Set<String> found = sorted.stream().map(SchedulerImplementationBuildItem::getImplementation)
                .collect(Collectors.toUnmodifiableSet());
        if (found.size() != implementations.size()) {
            throw new IllegalStateException("Invalid scheduler implementations detected: " + implementations);
        }
        DiscoveredImplementationsBuildItem discovered = new DiscoveredImplementationsBuildItem(
                sorted.get(0).getImplementation(), found,
                config.useCompositeScheduler());
        discoveredImplementations.produce(discovered);
        if (implementations.size() > 1 && config.useCompositeScheduler()) {
            // If multiple implementations are needed we have to register the CompositeScheduler, and
            // instruct the extensions that provide an implementation to modify the bean metadata, i.e. add the marker qualifier
            additionalBeans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(Constituent.class, CompositeScheduler.class).setUnremovable().build());
        }
    }

    @BuildStep
    void transformSchedulerBeans(DiscoveredImplementationsBuildItem discoveredImplementations,
            List<SchedulerImplementationBuildItem> implementations,
            BuildProducer<AnnotationsTransformerBuildItem> transformer) {
        if (discoveredImplementations.isCompositeSchedulerUsed()) {
            Map<DotName, String> implsToBeanClass = implementations.stream()
                    .collect(Collectors.toMap(SchedulerImplementationBuildItem::getSchedulerBeanClass,
                            SchedulerImplementationBuildItem::getImplementation));
            transformer.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation.forClasses()
                    .whenClass(c -> implsToBeanClass.containsKey(c.name()))
                    .transform(c -> {
                        c.add(AnnotationInstance.builder(Constituent.class).build());
                        c.add(AnnotationInstance.builder(Identifier.class)
                                .add("value", implsToBeanClass.get(c.declaration().asClass().name())).build());
                    })));
        }
    }

    @BuildStep
    void beans(DiscoveredImplementationsBuildItem discoveredImplementations,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(Scheduled.ApplicationNotRunning.class));
        if (discoveredImplementations.getImplementations().size() == 1
                || discoveredImplementations.isCompositeSchedulerUsed()) {
            // Quartz extension is not present or composite scheduler is used
            additionalBeans.produce(new AdditionalBeanBuildItem(SimpleScheduler.class));
        }
    }

    @BuildStep
    AutoAddScopeBuildItem autoAddScope() {
        // We add @Singleton to any bean class that has no scope annotation and declares at least one non-static method annotated with @Scheduled
        return AutoAddScopeBuildItem.builder()
                .anyMethodMatches(m -> !Modifier.isStatic(m.flags())
                        && (m.hasAnnotation(SchedulerDotNames.SCHEDULED_NAME)
                                || m.hasAnnotation(SchedulerDotNames.SCHEDULES_NAME)))
                .defaultScope(BuiltinScope.SINGLETON)
                .reason("Found non-static scheduled business methods").build();
    }

    @BuildStep
    void collectScheduledMethods(BeanArchiveIndexBuildItem beanArchives, BeanDiscoveryFinishedBuildItem beanDiscovery,
            TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<ScheduledBusinessMethodItem> scheduledBusinessMethods) {

        // First collect static scheduled methods
        Map<MethodInfo, List<AnnotationInstance>> staticScheduledMethods = new HashMap<>();
        List<AnnotationInstance> schedules = new ArrayList<>(
                beanArchives.getIndex().getAnnotations(SchedulerDotNames.SCHEDULED_NAME));
        for (AnnotationInstance annotationInstance : beanArchives.getIndex().getAnnotations(SchedulerDotNames.SCHEDULES_NAME)) {
            for (AnnotationInstance scheduledInstance : annotationInstance.value().asNestedArray()) {
                // We need to set the target of the containing instance
                schedules.add(AnnotationInstance.create(scheduledInstance.name(), annotationInstance.target(),
                        scheduledInstance.values()));
            }
        }
        for (AnnotationInstance annotationInstance : schedules) {
            if (annotationInstance.target().kind() != METHOD) {
                continue; // This should never happen as the annotation has @Target(METHOD)
            }
            MethodInfo method = annotationInstance.target().asMethod();
            ClassInfo declaringClass = method.declaringClass();
            if (!Modifier.isStatic(method.flags())
                    && (Modifier.isAbstract(declaringClass.flags()) || declaringClass.isInterface())) {
                throw new IllegalStateException(String.format(
                        "Non-static @Scheduled methods may not be declared on abstract classes and interfaces: %s() declared on %s",
                        method.name(), declaringClass.name()));
            }
            if (Modifier.isStatic(method.flags()) && !KotlinUtil.isSuspendMethod(method)) {
                List<AnnotationInstance> methodSchedules = staticScheduledMethods.get(method);
                if (methodSchedules == null) {
                    methodSchedules = new ArrayList<>();
                    staticScheduledMethods.put(method, methodSchedules);
                }
                methodSchedules.add(annotationInstance);
            }
        }

        for (Entry<MethodInfo, List<AnnotationInstance>> e : staticScheduledMethods.entrySet()) {
            MethodInfo method = e.getKey();
            scheduledBusinessMethods.produce(new ScheduledBusinessMethodItem(null, method, e.getValue(),
                    transformedAnnotations.hasAnnotation(method, SchedulerDotNames.NON_BLOCKING),
                    transformedAnnotations.hasAnnotation(method, SchedulerDotNames.RUN_ON_VIRTUAL_THREAD)));
            LOGGER.debugf("Found scheduled static method %s declared on %s", method, method.declaringClass().name());
        }

        // Then collect all business methods annotated with @Scheduled
        for (BeanInfo bean : beanDiscovery.beanStream().classBeans()) {
            collectScheduledMethods(beanArchives.getIndex(), transformedAnnotations, bean,
                    bean.getTarget().get().asClass(),
                    scheduledBusinessMethods);
        }
    }

    private void collectScheduledMethods(IndexView index, TransformedAnnotationsBuildItem transformedAnnotations, BeanInfo bean,
            ClassInfo beanClass, BuildProducer<ScheduledBusinessMethodItem> scheduledBusinessMethods) {

        for (MethodInfo method : beanClass.methods()) {
            if (Modifier.isStatic(method.flags())) {
                // Ignore static methods
                continue;
            }
            List<AnnotationInstance> schedules = null;
            AnnotationInstance scheduledAnnotation = transformedAnnotations.getAnnotation(method,
                    SchedulerDotNames.SCHEDULED_NAME);
            if (scheduledAnnotation != null) {
                schedules = List.of(scheduledAnnotation);
            } else {
                AnnotationInstance schedulesAnnotation = transformedAnnotations.getAnnotation(method,
                        SchedulerDotNames.SCHEDULES_NAME);
                if (schedulesAnnotation != null) {
                    schedules = new ArrayList<>();
                    for (AnnotationInstance scheduledInstance : schedulesAnnotation.value().asNestedArray()) {
                        // We need to set the target of the containing instance
                        schedules.add(AnnotationInstance.create(scheduledInstance.name(), schedulesAnnotation.target(),
                                scheduledInstance.values()));
                    }
                }
            }
            if (schedules != null) {
                scheduledBusinessMethods.produce(new ScheduledBusinessMethodItem(bean, method, schedules,
                        transformedAnnotations.hasAnnotation(method, SchedulerDotNames.NON_BLOCKING),
                        transformedAnnotations.hasAnnotation(method, SchedulerDotNames.RUN_ON_VIRTUAL_THREAD)));
                LOGGER.debugf("Found scheduled business method %s declared on %s", method, bean);
            }
        }
    }

    @BuildStep
    void validateScheduledBusinessMethods(SchedulerConfig config, List<ScheduledBusinessMethodItem> scheduledMethods,
            ValidationPhaseBuildItem validationPhase, BuildProducer<ValidationErrorBuildItem> validationErrors,
            Capabilities capabilities, BeanArchiveIndexBuildItem beanArchiveIndex,
            DiscoveredImplementationsBuildItem discoveredImplementations) {
        List<Throwable> errors = new ArrayList<>();
        Map<String, AnnotationInstance> encounteredIdentities = new HashMap<>();
        Set<String> methodDescriptions = new HashSet<>();

        for (ScheduledBusinessMethodItem scheduledMethod : scheduledMethods) {
            if (!methodDescriptions.add(scheduledMethod.getMethodDescription())) {
                errors.add(new IllegalStateException("Multiple @Scheduled methods of the same name declared on the same class: "
                        + scheduledMethod.getMethodDescription()));
                continue;
            }
            MethodInfo method = scheduledMethod.getMethod();
            if (Modifier.isAbstract(method.flags())) {
                errors.add(new IllegalStateException("@Scheduled method must not be abstract: "
                        + scheduledMethod.getMethodDescription()));
                continue;
            }
            if (Modifier.isPrivate(method.flags())) {
                errors.add(new IllegalStateException("@Scheduled method must not be private: "
                        + scheduledMethod.getMethodDescription()));
                continue;
            }

            if (scheduledMethod.isNonBlocking() && scheduledMethod.isRunOnVirtualThread()) {
                errors.add(new IllegalStateException("@Scheduled method cannot be non-blocking and annotated " +
                        "with @RunOnVirtualThread: " + scheduledMethod.getMethodDescription()));
            }

            boolean isSuspendMethod = KotlinUtil.isSuspendMethod(method);

            // Validate method params and return type
            List<Type> params = method.parameterTypes();
            int maxParamSize = isSuspendMethod ? 2 : 1;
            if (params.size() > maxParamSize
                    || (params.size() == maxParamSize && !params.get(0).equals(SCHEDULED_EXECUTION_TYPE))) {
                errors.add(new IllegalStateException(String.format(
                        "Invalid scheduled business method parameters %s [method: %s, bean: %s]", params,
                        method, scheduledMethod.getBean())));
            }
            if (!isValidReturnType(method)) {
                if (isSuspendMethod) {
                    errors.add(new IllegalStateException(
                            String.format(
                                    "Suspending scheduled business method must return Unit [method: %s, bean: %s]",
                                    method, scheduledMethod.getBean())));
                } else {
                    errors.add(new IllegalStateException(
                            String.format(
                                    "Scheduled business method must return void, CompletionStage<Void> or Uni<Void> [method: %s, bean: %s]",
                                    method, scheduledMethod.getBean())));
                }
            }
            // Validate cron() and every() expressions
            long checkPeriod = capabilities.isMissing(Capability.QUARTZ) ? SimpleScheduler.CHECK_PERIOD : 50;
            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(config.cronType()));
            for (AnnotationInstance scheduled : scheduledMethod.getSchedules()) {
                Throwable error = validateScheduled(parser, scheduled, encounteredIdentities, validationPhase.getContext(),
                        checkPeriod, beanArchiveIndex.getIndex(), discoveredImplementations);
                if (error != null) {
                    errors.add(error);
                }
            }
        }

        if (!errors.isEmpty()) {
            validationErrors.produce(new ValidationErrorBuildItem(errors));
        }
    }

    private boolean isValidReturnType(MethodInfo method) {
        Type returnType = method.returnType();
        if (returnType.kind() == Kind.VOID) {
            return true;
        }
        if (SchedulerDotNames.COMPLETION_STAGE.equals(returnType.name())
                && returnType.asParameterizedType().arguments().get(0).name().equals(SchedulerDotNames.VOID)) {
            return true;
        }
        if (SchedulerDotNames.UNI.equals(returnType.name())
                && returnType.asParameterizedType().arguments().get(0).name().equals(SchedulerDotNames.VOID)) {
            return true;
        }
        if (KotlinUtil.isSuspendMethod(method)
                && SchedulerDotNames.VOID.equals(KotlinUtil.determineReturnTypeOfSuspendMethod(method).name())) {
            return true;
        }
        return false;
    }

    @BuildStep
    public List<UnremovableBeanBuildItem> unremovableBeans() {
        // Beans annotated with @Scheduled should never be removed
        return List.of(new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(SchedulerDotNames.SCHEDULED_NAME)),
                new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(SchedulerDotNames.SCHEDULES_NAME)));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public FeatureBuildItem build(
            SchedulerRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            List<ScheduledBusinessMethodItem> scheduledMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            AnnotationProxyBuildItem annotationProxy,
            List<ForceStartSchedulerBuildItem> schedulerForcedStartItems,
            DiscoveredImplementationsBuildItem discoveredImplementations) {

        List<MutableScheduledMethod> scheduledMetadata = new ArrayList<>();
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, new Function<String, String>() {
            @Override
            public String apply(String name) {
                // org/acme/Foo_ScheduledInvoker_run_0000 -> org.acme.Foo
                int idx = name.indexOf(INVOKER_SUFFIX);
                if (idx != -1) {
                    name = name.substring(0, idx);
                }
                if (name.contains(NESTED_SEPARATOR)) {
                    name = name.replace(NESTED_SEPARATOR, "$");
                }
                return name;
            }
        });

        for (ScheduledBusinessMethodItem scheduledMethod : scheduledMethods) {
            MutableScheduledMethod metadata = new MutableScheduledMethod();
            String invokerClass = generateInvoker(scheduledMethod, classOutput);
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(invokerClass).constructors().methods().fields().build());
            metadata.setInvokerClassName(invokerClass);
            List<Scheduled> schedules = new ArrayList<>();
            for (AnnotationInstance scheduled : scheduledMethod.getSchedules()) {
                schedules.add(annotationProxy.builder(scheduled, Scheduled.class).build(classOutput));
            }
            metadata.setSchedules(schedules);
            metadata.setDeclaringClassName(scheduledMethod.getMethod().declaringClass().toString());
            metadata.setMethodName(scheduledMethod.getMethod().name());
            scheduledMetadata.add(metadata);
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(SchedulerContext.class).setRuntimeInit()
                .supplier(recorder.createContext(scheduledMetadata, !schedulerForcedStartItems.isEmpty(),
                        discoveredImplementations.getAutoImplementation()))
                .done());

        return new FeatureBuildItem(Feature.SCHEDULER);
    }

    @BuildStep
    public void metrics(SchedulerConfig config,
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {

        if (config.metricsEnabled() && metricsCapability.isPresent()) {
            DotName micrometerTimed = DotName.createSimple("io.micrometer.core.annotation.Timed");
            DotName mpTimed = DotName.createSimple("org.eclipse.microprofile.metrics.annotation.Timed");

            annotationsTransformer.produce(new AnnotationsTransformerBuildItem(AnnotationsTransformer.builder()
                    .appliesTo(METHOD)
                    .whenContainsAny(List.of(SchedulerDotNames.SCHEDULED_NAME, SchedulerDotNames.SCHEDULES_NAME))
                    .whenContainsNone(List.of(micrometerTimed,
                            mpTimed, DotName.createSimple("org.eclipse.microprofile.metrics.annotation.SimplyTimed")))
                    .transform(context -> {
                        // Transform a @Scheduled method that has no metrics timed annotation
                        MethodInfo scheduledMethod = context.getTarget().asMethod();
                        if (metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
                            // Micrometer
                            context.transform()
                                    .add(micrometerTimed, createStringValue("value", "scheduled.methods"))
                                    .add(micrometerTimed, createStringValue("value", "scheduled.methods.running"),
                                            createBooleanValue("longTask", true))
                                    .done();
                            LOGGER.debugf("Added Micrometer @Timed to a @Scheduled method %s#%s()",
                                    scheduledMethod.declaringClass().name(),
                                    scheduledMethod.name());
                        } else if (metricsCapability.get().metricsSupported(MetricsFactory.MP_METRICS)) {
                            // MP metrics
                            context.transform()
                                    .add(mpTimed,
                                            createArrayValue("tags",
                                                    new AnnotationValue[] { createStringValue("scheduled", "scheduled=true") }))
                                    .done();
                            LOGGER.debugf("Added MP Metrics @Timed to a @Scheduled method %s#%s()",
                                    scheduledMethod.declaringClass().name(),
                                    scheduledMethod.name());
                        }
                    })));
        }
    }

    private String generateInvoker(ScheduledBusinessMethodItem scheduledMethod, ClassOutput classOutput) {

        BeanInfo bean = scheduledMethod.getBean();
        MethodInfo method = scheduledMethod.getMethod();
        boolean isStatic = Modifier.isStatic(method.flags());
        ClassInfo implClazz = isStatic ? method.declaringClass() : bean.getImplClazz();

        String baseName;
        if (implClazz.enclosingClass() != null) {
            baseName = DotNames.simpleName(implClazz.enclosingClass()) + NESTED_SEPARATOR
                    + DotNames.simpleName(implClazz);
        } else {
            baseName = DotNames.simpleName(implClazz.name());
        }
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameterTypes()) {
            sigBuilder.append(i.name().toString());
        }
        String generatedName = DotNames.internalPackageNameWithTrailingSlash(implClazz.name()) + baseName
                + INVOKER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());

        boolean isSuspendMethod = KotlinUtil.isSuspendMethod(method);

        ClassCreator invokerCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(isSuspendMethod ? SchedulerDotNames.ABSTRACT_COROUTINE_INVOKER.toString()
                        : DefaultInvoker.class.getName())
                .build();

        MethodCreator invoke;
        if (isSuspendMethod) {
            invoke = invokerCreator
                    .getMethodCreator("invokeBean", Object.class.getName(),
                            ScheduledExecution.class.getName(), SchedulerDotNames.CONTINUATION.toString());
        } else {
            // The descriptor is: CompletionStage invoke(ScheduledExecution execution)
            invoke = invokerCreator.getMethodCreator("invokeBean", CompletionStage.class, ScheduledExecution.class);
        }

        // Use a try-catch block and return failed future if an exception is thrown
        TryBlock tryBlock = invoke.tryBlock();
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        if (isSuspendMethod) {
            catchBlock.throwException(catchBlock.getCaughtException());
        } else {
            catchBlock.returnValue(catchBlock.invokeStaticMethod(
                    MethodDescriptor.ofMethod(CompletableFuture.class, "failedStage", CompletionStage.class, Throwable.class),
                    catchBlock.getCaughtException()));
        }

        String returnTypeStr = DescriptorUtils.typeToString(method.returnType());
        ResultHandle res;
        if (isStatic) {
            if (implClazz.isInterface()) {
                if (method.parameterTypes().isEmpty()) {
                    res = tryBlock.invokeStaticInterfaceMethod(
                            MethodDescriptor.ofMethod(implClazz.name().toString(), method.name(), returnTypeStr));
                } else {
                    res = tryBlock.invokeStaticInterfaceMethod(
                            MethodDescriptor.ofMethod(implClazz.name().toString(), method.name(), returnTypeStr,
                                    ScheduledExecution.class),
                            tryBlock.getMethodParam(0));
                }
            } else {
                if (method.parameterTypes().isEmpty()) {
                    res = tryBlock.invokeStaticMethod(
                            MethodDescriptor.ofMethod(implClazz.name().toString(), method.name(), returnTypeStr));
                } else {
                    res = tryBlock.invokeStaticMethod(
                            MethodDescriptor.ofMethod(implClazz.name().toString(), method.name(), returnTypeStr,
                                    ScheduledExecution.class),
                            tryBlock.getMethodParam(0));
                }
            }
        } else {
            // InjectableBean<Foo> bean = Arc.container().bean("foo1");
            // InstanceHandle<Foo> handle = Arc.container().instance(bean);
            // handle.get().ping();
            ResultHandle containerHandle = tryBlock
                    .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
            ResultHandle beanHandle = tryBlock.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(ArcContainer.class, "bean", InjectableBean.class, String.class),
                    containerHandle, tryBlock.load(bean.getIdentifier()));
            ResultHandle instanceHandle = tryBlock.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, InjectableBean.class),
                    containerHandle, beanHandle);
            ResultHandle beanInstanceHandle = tryBlock
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                            instanceHandle);

            if (isSuspendMethod) {
                if (method.parametersCount() == 1) {
                    res = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(implClazz.name().toString(), method.name(), Object.class.getName(),
                                    SchedulerDotNames.CONTINUATION.toString()),
                            beanInstanceHandle, tryBlock.getMethodParam(1));
                } else {
                    res = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(implClazz.name().toString(), method.name(), Object.class.getName(),
                                    ScheduledExecution.class.getName(), SchedulerDotNames.CONTINUATION.toString()),
                            beanInstanceHandle, tryBlock.getMethodParam(0), tryBlock.getMethodParam(1));
                }
            } else {
                if (method.parameterTypes().isEmpty()) {
                    res = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(implClazz.name().toString(), method.name(), returnTypeStr),
                            beanInstanceHandle);
                } else {
                    res = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(implClazz.name().toString(), method.name(), returnTypeStr,
                                    ScheduledExecution.class),
                            beanInstanceHandle, tryBlock.getMethodParam(0));
                }
            }
            // handle.destroy() - destroy dependent instance afterwards
            if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
                tryBlock.invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "destroy", void.class),
                        instanceHandle);
            }
        }

        if (res == null) {
            // If the return type is void then return a new completed stage
            res = tryBlock.invokeStaticMethod(
                    MethodDescriptor.ofMethod(CompletableFuture.class, "completedStage", CompletionStage.class, Object.class),
                    tryBlock.loadNull());
        } else if (method.returnType().name().equals(SchedulerDotNames.UNI)) {
            // Subscribe to the returned Uni
            res = tryBlock.invokeInterfaceMethod(MethodDescriptor.ofMethod(SchedulerDotNames.UNI.toString(),
                    "subscribeAsCompletionStage", CompletableFuture.class), res);
        }

        tryBlock.returnValue(res);

        if (scheduledMethod.isNonBlocking()) {
            MethodCreator isBlocking = invokerCreator.getMethodCreator("isBlocking", boolean.class);
            isBlocking.returnValue(isBlocking.load(false));
            isBlocking.close();
        }

        if (scheduledMethod.isRunOnVirtualThread()) {
            MethodCreator isRunOnVirtualThread = invokerCreator.getMethodCreator("isRunningOnVirtualThread", boolean.class);
            isRunOnVirtualThread.returnValue(isRunOnVirtualThread.load(true));
            isRunOnVirtualThread.close();
        }

        invokerCreator.close();
        return generatedName.replace('/', '.');
    }

    private Throwable validateScheduled(CronParser parser, AnnotationInstance schedule,
            Map<String, AnnotationInstance> encounteredIdentities, BeanDeploymentValidator.ValidationContext validationContext,
            long checkPeriod, IndexView index, DiscoveredImplementationsBuildItem discoveredImplementations) {
        MethodInfo method = schedule.target().asMethod();
        AnnotationValue cronValue = schedule.value("cron");
        AnnotationValue everyValue = schedule.value("every");
        if (cronValue != null && !cronValue.asString().trim().isEmpty()) {
            String cron = cronValue.asString().trim();
            if (!SchedulerUtils.isConfigValue(cron)) {
                try {
                    parser.parse(cron).validate();
                } catch (IllegalArgumentException e) {
                    return new IllegalStateException(errorMessage("Invalid cron() expression", schedule, method), e);
                }
                if (everyValue != null && !everyValue.asString().trim().isEmpty()) {
                    LOGGER.warnf(
                            "%s declared on %s#%s() defines both cron() and every() - the cron expression takes precedence",
                            schedule, method.declaringClass().name(), method.name());
                }
            }
            // Validate the time zone ID
            AnnotationValue timeZoneValue = schedule.value("timeZone");
            if (timeZoneValue != null) {
                String timeZone = timeZoneValue.asString();
                if (!SchedulerUtils.isConfigValue(timeZone) && !timeZone.equals(Scheduled.DEFAULT_TIMEZONE)) {
                    try {
                        ZoneId.of(timeZone);
                    } catch (Exception e) {
                        return new IllegalStateException(errorMessage("Invalid timeZone()", schedule, method), e);
                    }
                }
            }

        } else {
            if (everyValue != null && !everyValue.asString().trim().isEmpty()) {
                String every = everyValue.asString().trim();
                if (!SchedulerUtils.isConfigValue(every)) {
                    if (Character.isDigit(every.charAt(0))) {
                        every = "PT" + every;
                    }
                    try {
                        Duration period = Duration.parse(every);
                        if (period.toMillis() < checkPeriod) {
                            LOGGER.warnf(
                                    "An every() value less than %s ms is not supported - the scheduled job will be executed with a delay: %s declared on %s#%s()",
                                    checkPeriod, schedule, method.declaringClass().name(), method.name());
                        }
                    } catch (Exception e) {
                        return new IllegalStateException(errorMessage("Invalid every() expression", schedule, method), e);
                    }
                }
            } else {
                return new IllegalStateException("@Scheduled must declare either cron() or every(): " + schedule);
            }
        }
        AnnotationValue delay = schedule.value("delay");
        AnnotationValue delayedValue = schedule.value("delayed");
        if (delay == null || delay.asLong() <= 0) {
            if (delayedValue != null && !delayedValue.asString().trim().isEmpty()) {
                String delayed = delayedValue.asString().trim();
                if (!SchedulerUtils.isConfigValue(delayed)) {
                    if (Character.isDigit(delayed.charAt(0))) {
                        delayed = "PT" + delayed;
                    }
                    try {
                        Duration.parse(delayed);
                    } catch (Exception e) {
                        return new IllegalStateException(errorMessage("Invalid delayed() expression", schedule, method), e);
                    }
                }

            }
        } else {
            if (delayedValue != null && !delayedValue.asString().trim().isEmpty()) {
                LOGGER.warnf(
                        "%s declared on %s#%s() defines both delay() and delayed() - the delayed() value is ignored",
                        schedule, method.declaringClass().name(), method.name());
            }
        }

        AnnotationValue identityValue = schedule.value("identity");
        if (identityValue != null) {
            String identity = SchedulerUtils.lookUpPropertyValue(identityValue.asString());
            AnnotationInstance previousInstanceWithSameIdentity = encounteredIdentities.get(identity);
            if (previousInstanceWithSameIdentity != null) {
                String message = String.format("The identity: \"%s\" on: %s is not unique and it has already bean used by : %s",
                        identity, schedule, previousInstanceWithSameIdentity);
                return new IllegalStateException(message);
            } else {
                encounteredIdentities.put(identity, schedule);
            }
        }

        AnnotationValue skipExecutionIfValue = schedule.value("skipExecutionIf");
        if (skipExecutionIfValue != null) {
            DotName skipPredicate = skipExecutionIfValue.asClass().name();
            if (SchedulerDotNames.SKIP_NEVER_NAME.equals(skipPredicate)) {
                return null;
            }
            List<BeanInfo> beans = validationContext.beans().withBeanType(skipPredicate).collect();
            if (beans.size() > 1) {
                String message = String.format(
                        "There must be exactly one bean that matches the skip predicate: \"%s\" on: %s; beans: %s",
                        skipPredicate, schedule, beans);
                return new IllegalStateException(message);
            } else if (beans.isEmpty()) {
                ClassInfo skipPredicateClass = index.getClassByName(skipPredicate);
                if (skipPredicateClass != null) {
                    MethodInfo noArgsConstructor = skipPredicateClass.method("<init>");
                    if (noArgsConstructor == null || !Modifier.isPublic(noArgsConstructor.flags())) {
                        return new IllegalStateException(
                                "The skip predicate class must declare a public no-args constructor: " + skipPredicateClass);
                    }
                }
            }
        }

        AnnotationValue executeWithValue = schedule.value("executeWith");
        if (executeWithValue != null) {
            String implementation = executeWithValue.asString();
            if (!Scheduled.AUTO.equals(implementation)) {
                if (!discoveredImplementations.getImplementations().contains(implementation)) {
                    return new IllegalStateException(
                            "The required scheduler implementation was not discovered in application: " + implementation);
                } else if (!discoveredImplementations.isCompositeSchedulerUsed()
                        && !discoveredImplementations.isAutoImplementation(implementation)) {
                    return new IllegalStateException(
                            "The required scheduler implementation is not available because the composite scheduler is not used: "
                                    + implementation);
                }
            }
        }
        return null;
    }

    private static String errorMessage(String base, AnnotationInstance scheduled, MethodInfo method) {
        return String.format("%s: %s declared on %s#%s()", base, scheduled, method.declaringClass().name(), method.name());
    }

    @BuildStep
    UnremovableBeanBuildItem unremoveableSkipPredicates() {
        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanTypeExclusion(SchedulerDotNames.SKIP_PREDICATE));
    }

    @BuildStep
    void produceCoroutineScope(BuildProducer<AdditionalBeanBuildItem> buildItemBuildProducer) {
        if (!QuarkusClassLoader.isClassPresentAtRuntime("kotlinx.coroutines.CoroutineScope")) {
            return;
        }

        buildItemBuildProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass("io.quarkus.scheduler.kotlin.runtime.ApplicationCoroutineScope")
                .setUnremovable().build());
    }

}

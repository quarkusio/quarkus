package io.quarkus.scheduler.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
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
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.runtime.BeanLookupSupplier;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.runtime.ScheduledInvoker;
import io.quarkus.scheduler.runtime.ScheduledMethodMetadata;
import io.quarkus.scheduler.runtime.SchedulerConfig;
import io.quarkus.scheduler.runtime.SchedulerContext;
import io.quarkus.scheduler.runtime.SchedulerRecorder;
import io.quarkus.scheduler.runtime.SimpleScheduler;
import io.quarkus.scheduler.runtime.devconsole.SchedulerDevConsoleRecorder;
import io.quarkus.scheduler.runtime.util.SchedulerUtils;

/**
 * @author Martin Kouba
 */
public class SchedulerProcessor {

    private static final Logger LOGGER = Logger.getLogger(SchedulerProcessor.class);

    static final DotName SCHEDULED_NAME = DotName.createSimple(Scheduled.class.getName());
    static final DotName SCHEDULES_NAME = DotName.createSimple(Scheduled.Schedules.class.getName());

    static final Type SCHEDULED_EXECUTION_TYPE = Type.create(DotName.createSimple(ScheduledExecution.class.getName()),
            Kind.CLASS);

    static final String INVOKER_SUFFIX = "_ScheduledInvoker";
    static final String NESTED_SEPARATOR = "$_";

    @BuildStep
    void beans(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (capabilities.isMissing(Capability.QUARTZ)) {
            additionalBeans.produce(new AdditionalBeanBuildItem(SimpleScheduler.class));
        }
    }

    @BuildStep
    AutoAddScopeBuildItem autoAddScope() {
        return AutoAddScopeBuildItem.builder().containsAnnotations(SCHEDULED_NAME, SCHEDULES_NAME)
                .defaultScope(BuiltinScope.SINGLETON)
                .reason("Found scheduled business methods").build();
    }

    @BuildStep
    void collectScheduledMethods(BeanArchiveIndexBuildItem beanArchives, BeanDiscoveryFinishedBuildItem beanDiscovery,
            TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<ScheduledBusinessMethodItem> scheduledBusinessMethods) {

        // We need to collect all business methods annotated with @Scheduled first
        for (BeanInfo bean : beanDiscovery.beanStream().classBeans()) {
            collectScheduledMethods(beanArchives.getIndex(), transformedAnnotations, bean,
                    bean.getTarget().get().asClass(),
                    scheduledBusinessMethods);
        }
    }

    private void collectScheduledMethods(IndexView index, TransformedAnnotationsBuildItem transformedAnnotations, BeanInfo bean,
            ClassInfo beanClass, BuildProducer<ScheduledBusinessMethodItem> scheduledBusinessMethods) {

        for (MethodInfo method : beanClass.methods()) {
            List<AnnotationInstance> schedules = null;
            AnnotationInstance scheduledAnnotation = transformedAnnotations.getAnnotation(method, SCHEDULED_NAME);
            if (scheduledAnnotation != null) {
                schedules = Collections.singletonList(scheduledAnnotation);
            } else {
                AnnotationInstance schedulesAnnotation = transformedAnnotations.getAnnotation(method, SCHEDULES_NAME);
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
                scheduledBusinessMethods.produce(new ScheduledBusinessMethodItem(bean, method, schedules));
                LOGGER.debugf("Found scheduled business method %s declared on %s", method, bean);
            }
        }

        DotName superClassName = beanClass.superName();
        if (superClassName != null) {
            ClassInfo superClass = index.getClassByName(superClassName);
            if (superClass != null) {
                collectScheduledMethods(index, transformedAnnotations, bean, superClass, scheduledBusinessMethods);
            }
        }
    }

    @BuildStep
    void validateScheduledBusinessMethods(SchedulerConfig config, List<ScheduledBusinessMethodItem> scheduledMethods,
            ValidationPhaseBuildItem validationPhase, BuildProducer<ValidationErrorBuildItem> validationErrors) {
        List<Throwable> errors = new ArrayList<>();
        Map<String, AnnotationInstance> encounteredIdentities = new HashMap<>();

        for (ScheduledBusinessMethodItem scheduledMethod : scheduledMethods) {
            MethodInfo method = scheduledMethod.getMethod();

            if (Modifier.isPrivate(method.flags()) || Modifier.isStatic(method.flags())) {
                errors.add(new IllegalStateException("@Scheduled method must be non-private and non-static: "
                        + method.declaringClass().name() + "#" + method.name() + "()"));
                continue;
            }

            // Validate method params and return type
            List<Type> params = method.parameters();
            if (params.size() > 1
                    || (params.size() == 1 && !params.get(0).equals(SCHEDULED_EXECUTION_TYPE))) {
                errors.add(new IllegalStateException(String.format(
                        "Invalid scheduled business method parameters %s [method: %s, bean: %s]", params,
                        method, scheduledMethod.getBean())));
            }
            if (!method.returnType().kind().equals(Type.Kind.VOID)) {
                errors.add(new IllegalStateException(
                        String.format("Scheduled business method must return void [method: %s, bean: %s]",
                                method, scheduledMethod.getBean())));
            }
            // Validate cron() and every() expressions
            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(config.cronType));
            for (AnnotationInstance scheduled : scheduledMethod.getSchedules()) {
                Throwable error = validateScheduled(parser, scheduled, encounteredIdentities);
                if (error != null) {
                    errors.add(error);
                }
            }
        }

        if (!errors.isEmpty()) {
            validationErrors.produce(new ValidationErrorBuildItem(errors));
        }
    }

    @BuildStep
    public List<UnremovableBeanBuildItem> unremovableBeans() {
        // Beans annotated with @Scheduled should never be removed
        return Arrays.asList(new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(SCHEDULED_NAME)),
                new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(SCHEDULES_NAME)));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public FeatureBuildItem build(SchedulerConfig config, BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            SchedulerRecorder recorder, List<ScheduledBusinessMethodItem> scheduledMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            AnnotationProxyBuildItem annotationProxy, ExecutorBuildItem executor) {

        List<ScheduledMethodMetadata> scheduledMetadata = new ArrayList<>();
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
            ScheduledMethodMetadata metadata = new ScheduledMethodMetadata();
            String invokerClass = generateInvoker(scheduledMethod, classOutput);
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, invokerClass));
            metadata.setInvokerClassName(invokerClass);
            List<Scheduled> schedules = new ArrayList<>();
            for (AnnotationInstance scheduled : scheduledMethod.getSchedules()) {
                schedules.add(annotationProxy.builder(scheduled, Scheduled.class).build(classOutput));
            }
            metadata.setSchedules(schedules);
            metadata.setMethodDescription(
                    scheduledMethod.getMethod().declaringClass() + "#" + scheduledMethod.getMethod().name());
            scheduledMetadata.add(metadata);
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(SchedulerContext.class).setRuntimeInit()
                .supplier(recorder.createContext(config, executor.getExecutorProxy(), scheduledMetadata))
                .done());

        return new FeatureBuildItem(Feature.SCHEDULER);
    }

    @BuildStep
    public void devConsoleInfo(BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> infos) {
        infos.produce(new DevConsoleRuntimeTemplateInfoBuildItem("schedulerContext",
                new BeanLookupSupplier(SchedulerContext.class)));
        infos.produce(new DevConsoleRuntimeTemplateInfoBuildItem("scheduler",
                new BeanLookupSupplier(Scheduler.class)));
    }

    @BuildStep
    @Record(value = STATIC_INIT, optional = true)
    DevConsoleRouteBuildItem invokeEndpoint(SchedulerDevConsoleRecorder recorder) {
        return new DevConsoleRouteBuildItem("schedules", "POST", recorder.invokeHandler());
    }

    private String generateInvoker(ScheduledBusinessMethodItem scheduledMethod, ClassOutput classOutput) {

        BeanInfo bean = scheduledMethod.getBean();
        MethodInfo method = scheduledMethod.getMethod();

        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = DotNames.simpleName(bean.getImplClazz().enclosingClass()) + NESTED_SEPARATOR
                    + DotNames.simpleName(bean.getImplClazz());
        } else {
            baseName = DotNames.simpleName(bean.getImplClazz().name());
        }
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        String generatedName = DotNames.internalPackageNameWithTrailingSlash(bean.getImplClazz().name()) + baseName
                + INVOKER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());

        ClassCreator invokerCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(ScheduledInvoker.class)
                .build();

        // The descriptor is: void invokeBean(Object execution)
        MethodCreator invoke = invokerCreator.getMethodCreator("invokeBean", void.class, Object.class)
                .addException(Exception.class);
        // InjectableBean<Foo: bean = Arc.container().bean("1");
        // InstanceHandle<Foo> handle = Arc.container().instance(bean);
        // handle.get().ping();
        ResultHandle containerHandle = invoke
                .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle beanHandle = invoke.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "bean", InjectableBean.class, String.class),
                containerHandle, invoke.load(bean.getIdentifier()));
        ResultHandle instanceHandle = invoke.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, InjectableBean.class),
                containerHandle, beanHandle);
        ResultHandle beanInstanceHandle = invoke
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);
        if (method.parameters().isEmpty()) {
            invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class),
                    beanInstanceHandle);
        } else {
            invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class,
                            ScheduledExecution.class),
                    beanInstanceHandle, invoke.getMethodParam(0));
        }
        // handle.destroy() - destroy dependent instance afterwards
        if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
            invoke.invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "destroy", void.class),
                    instanceHandle);
        }
        invoke.returnValue(null);

        invokerCreator.close();
        return generatedName.replace('/', '.');
    }

    private Throwable validateScheduled(CronParser parser, AnnotationInstance schedule,
            Map<String, AnnotationInstance> encounteredIdentities) {
        MethodInfo method = schedule.target().asMethod();
        AnnotationValue cronValue = schedule.value("cron");
        AnnotationValue everyValue = schedule.value("every");
        if (cronValue != null && !cronValue.asString().trim().isEmpty()) {
            String cron = cronValue.asString().trim();
            if (!SchedulerUtils.isConfigValue(cron)) {
                try {
                    parser.parse(cron).validate();
                } catch (IllegalArgumentException e) {
                    return new IllegalStateException("Invalid cron() expression on: " + schedule, e);
                }
                if (everyValue != null && !everyValue.asString().trim().isEmpty()) {
                    LOGGER.warnf(
                            "%s declared on %s#%s() defines both cron() and every() - the cron expression takes precedence",
                            schedule, method.declaringClass().name(), method.name());
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
                        Duration.parse(every);
                    } catch (Exception e) {
                        return new IllegalStateException("Invalid every() expression on: " + schedule, e);
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
                        return new IllegalStateException("Invalid delayed() expression on: " + schedule, e);
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
        return null;
    }

}

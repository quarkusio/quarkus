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
package io.quarkus.scheduler.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;
import org.quartz.CronExpression;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDeploymentValidatorBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.runtime.QuartzScheduler;
import io.quarkus.scheduler.runtime.ScheduledInvoker;
import io.quarkus.scheduler.runtime.SchedulerConfiguration;
import io.quarkus.scheduler.runtime.SchedulerDeploymentTemplate;

/**
 * @author Martin Kouba
 */
public class SchedulerProcessor {

    private static final Logger LOGGER = Logger.getLogger("io.quarkus.scheduler.deployment.processor");

    static final DotName SCHEDULED_NAME = DotName.createSimple(Scheduled.class.getName());
    static final DotName SCHEDULES_NAME = DotName.createSimple(Scheduled.Schedules.class.getName());

    static final Type SCHEDULED_EXECUTION_TYPE = Type.create(DotName.createSimple(ScheduledExecution.class.getName()),
            Kind.CLASS);

    static final String INVOKER_SUFFIX = "_ScheduledInvoker";

    @BuildStep
    List<AdditionalBeanBuildItem> beans() {
        List<AdditionalBeanBuildItem> beans = new ArrayList<>();
        beans.add(new AdditionalBeanBuildItem(SchedulerConfiguration.class));
        beans.add(new AdditionalBeanBuildItem(QuartzScheduler.class));
        return beans;
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                if (context.getAnnotations().isEmpty()) {
                    // Class with no annotations but with @Scheduled method
                    if (context.getTarget().asClass().annotations().containsKey(SCHEDULED_NAME)
                            || context.getTarget().asClass().annotations().containsKey(SCHEDULES_NAME)) {
                        LOGGER.debugf("Found scheduled business methods on a class %s with no annotations - adding @Singleton",
                                context.getTarget());
                        context.transform().add(Singleton.class).done();
                    }
                }
            }
        });
    }

    @BuildStep
    List<ReflectiveClassBuildItem> reflectiveClasses() {
        List<ReflectiveClassBuildItem> reflectiveClasses = new ArrayList<>();
        reflectiveClasses.add(new ReflectiveClassBuildItem(false, false, CascadingClassLoadHelper.class.getName()));
        reflectiveClasses.add(new ReflectiveClassBuildItem(true, false, SimpleThreadPool.class.getName()));
        reflectiveClasses.add(new ReflectiveClassBuildItem(true, false, RAMJobStore.class.getName()));
        return reflectiveClasses;
    }

    @BuildStep
    BeanDeploymentValidatorBuildItem beanDeploymentValidator(
            BuildProducer<ScheduledBusinessMethodItem> scheduledBusinessMethods) {

        return new BeanDeploymentValidatorBuildItem(new BeanDeploymentValidator() {

            @Override
            public void validate(ValidationContext validationContext) {

                AnnotationStore annotationStore = validationContext.get(Key.ANNOTATION_STORE);

                // We need to collect all business methods annotated with @Scheduled first
                for (BeanInfo bean : validationContext.get(Key.BEANS)) {
                    if (bean.isClassBean()) {
                        // TODO: inherited business methods?
                        for (MethodInfo method : bean.getTarget().get().asClass().methods()) {

                            List<AnnotationInstance> schedules;

                            AnnotationInstance scheduledAnnotation = annotationStore.getAnnotation(method, SCHEDULED_NAME);
                            if (scheduledAnnotation != null) {
                                schedules = Collections.singletonList(scheduledAnnotation);
                            } else {
                                AnnotationInstance scheduledsAnnotation = annotationStore.getAnnotation(method, SCHEDULES_NAME);
                                if (scheduledsAnnotation != null) {
                                    schedules = new ArrayList<>();
                                    for (AnnotationInstance scheduledInstance : scheduledsAnnotation.value().asNestedArray()) {
                                        schedules.add(scheduledInstance);
                                    }
                                } else {
                                    schedules = null;
                                }
                            }

                            if (schedules != null) {
                                // Validate method params and return type
                                List<Type> params = method.parameters();
                                if (params.size() > 1
                                        || (params.size() == 1 && !params.get(0).equals(SCHEDULED_EXECUTION_TYPE))) {
                                    throw new IllegalStateException(String.format(
                                            "Invalid scheduled business method parameters %s [method: %s, bean:%s", params,
                                            method, bean));
                                }
                                if (!method.returnType().kind().equals(Type.Kind.VOID)) {
                                    throw new IllegalStateException(
                                            String.format("Scheduled business method must return void [method: %s, bean:%s",
                                                    method.returnType(), method, bean));
                                }
                                // Validate cron() and every() expressions
                                for (AnnotationInstance scheduled : schedules) {
                                    validateScheduled(validationContext, scheduled);
                                }
                                scheduledBusinessMethods.produce(new ScheduledBusinessMethodItem(bean, method, schedules));
                                LOGGER.debugf("Found scheduled business method %s declared on %s", method, bean);
                            }
                        }

                    }
                }
            }
        });
    }

    @BuildStep
    public List<UnremovableBeanBuildItem> unremovableBeans() {
        return Arrays.asList(new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(SCHEDULED_NAME)),
                new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(SCHEDULES_NAME)));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(SchedulerDeploymentTemplate template, BeanContainerBuildItem beanContainer,
            List<ScheduledBusinessMethodItem> scheduledBusinessMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClass, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<FeatureBuildItem> feature, AnnotationProxyBuildItem annotationProxy) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SCHEDULER));
        List<Map<String, Object>> scheduleConfigurations = new ArrayList<>();
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedClass.produce(new GeneratedClassBuildItem(true, name, data));
            }
        };

        for (ScheduledBusinessMethodItem businessMethod : scheduledBusinessMethods) {
            Map<String, Object> config = new HashMap<>();
            String invokerClass = generateInvoker(businessMethod.getBean(), businessMethod.getMethod(), classOutput);
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, invokerClass));
            config.put(SchedulerDeploymentTemplate.INVOKER_KEY, invokerClass);
            List<Scheduled> schedules = new ArrayList<>();
            for (AnnotationInstance scheduled : businessMethod.getSchedules()) {
                schedules.add(annotationProxy.from(scheduled, Scheduled.class));
            }
            config.put(SchedulerDeploymentTemplate.SCHEDULES_KEY, schedules);
            config.put(SchedulerDeploymentTemplate.DESC_KEY,
                    businessMethod.getMethod().declaringClass() + "#" + businessMethod.getMethod().name());
            scheduleConfigurations.add(config);
        }
        template.registerSchedules(scheduleConfigurations, beanContainer.getValue());
    }

    @BuildStep
    public void logCleanup(BuildProducer<LogCleanupFilterBuildItem> logCleanupFilter) {
        logCleanupFilter.produce(new LogCleanupFilterBuildItem("org.quartz.impl.StdSchedulerFactory",
                "Quartz scheduler version:",
                // no need to log if it's the default
                "Using default implementation for",
                "Quartz scheduler 'DefaultQuartzScheduler'"));

        logCleanupFilter.produce(new LogCleanupFilterBuildItem("org.quartz.core.QuartzScheduler",
                "Quartz Scheduler v",
                "JobFactory set to:",
                "Scheduler meta-data:",
                // no need to log if it's the default
                "Scheduler DefaultQuartzScheduler"));

        logCleanupFilter.produce(new LogCleanupFilterBuildItem("org.quartz.simpl.RAMJobStore",
                "RAMJobStore initialized."));

        logCleanupFilter.produce(new LogCleanupFilterBuildItem("org.quartz.core.SchedulerSignalerImpl",
                "Initialized Scheduler Signaller of type"));
    }

    private String generateInvoker(BeanInfo bean, MethodInfo method, ClassOutput classOutput) {

        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = DotNames.simpleName(bean.getImplClazz().enclosingClass()) + "_"
                    + DotNames.simpleName(bean.getImplClazz().name());
        } else {
            baseName = DotNames.simpleName(bean.getImplClazz().name());
        }
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        String targetPackage = DotNames.packageName(bean.getImplClazz().name());
        String generatedName = targetPackage.replace('.', '/') + "/" + baseName + INVOKER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());

        ClassCreator invokerCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(ScheduledInvoker.class)
                .build();

        MethodCreator invoke = invokerCreator.getMethodCreator("invoke", void.class, ScheduledExecution.class);
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

    private void validateScheduled(ValidationContext validationContext, AnnotationInstance schedule) {
        AnnotationValue cronValue = schedule.value("cron");
        if (cronValue != null && !cronValue.asString().trim().isEmpty()) {
            String cron = cronValue.asString().trim();
            if (SchedulerConfiguration.isConfigValue(cron)) {
                // Don't validate config property
                return;
            }
            try {
                new CronExpression(cron);
            } catch (ParseException e) {
                validationContext
                        .addDeploymentProblem(new IllegalStateException("Invalid cron() expression on: " + schedule, e));
            }
        } else {
            AnnotationValue everyValue = schedule.value("every");
            if (everyValue != null && !everyValue.asString().trim().isEmpty()) {
                String every = everyValue.asString().trim();
                if (SchedulerConfiguration.isConfigValue(every)) {
                    return;
                }
                if (Character.isDigit(every.charAt(0))) {
                    every = "PT" + every;
                }
                try {
                    Duration.parse(every);
                } catch (Exception e) {
                    validationContext
                            .addDeploymentProblem(new IllegalStateException("Invalid every() expression on: " + schedule, e));
                }
            } else {
                validationContext.addDeploymentProblem(
                        new IllegalStateException("@Scheduled must declare either cron() or every(): " + schedule));
            }
        }
    }

}

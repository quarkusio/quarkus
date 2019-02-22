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
package org.jboss.shamrock.scheduler.deployment;

import static org.jboss.shamrock.deployment.annotations.ExecutionTime.STATIC_INIT;

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
import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.InjectableBean;
import org.jboss.protean.arc.InstanceHandle;
import org.jboss.protean.arc.processor.AnnotationStore;
import org.jboss.protean.arc.processor.AnnotationsTransformer;
import org.jboss.protean.arc.processor.BeanDeploymentValidator;
import org.jboss.protean.arc.processor.BeanDeploymentValidator.ValidationContext;
import org.jboss.protean.arc.processor.BeanInfo;
import org.jboss.protean.arc.processor.DotNames;
import org.jboss.protean.arc.processor.ScopeInfo;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import org.jboss.shamrock.arc.deployment.AnnotationsTransformerBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerBuildItem;
import org.jboss.shamrock.arc.deployment.BeanDeploymentValidatorBuildItem;
import org.jboss.shamrock.arc.deployment.UnremovableBeanBuildItem;
import org.jboss.shamrock.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.AnnotationProxyBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.util.HashUtil;
import org.jboss.shamrock.scheduler.api.Scheduled;
import org.jboss.shamrock.scheduler.api.ScheduledExecution;
import org.jboss.shamrock.scheduler.runtime.QuartzScheduler;
import org.jboss.shamrock.scheduler.runtime.ScheduledInvoker;
import org.jboss.shamrock.scheduler.runtime.SchedulerConfiguration;
import org.jboss.shamrock.scheduler.runtime.SchedulerDeploymentTemplate;
import org.quartz.CronExpression;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

/**
 *
 * @author Martin Kouba
 */
public class SchedulerProcessor {

    private static final Logger LOGGER = Logger.getLogger("org.jboss.shamrock.scheduler.deployment.processor");

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
        if (bean.getScope() == ScopeInfo.DEPENDENT) {
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
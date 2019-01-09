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

import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.jboss.protean.arc.processor.BeanInfo;
import org.jboss.protean.arc.processor.DotNames;
import org.jboss.protean.arc.processor.ScopeInfo;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.arc.deployment.AnnotationsTransformerBuildItem;
import org.jboss.shamrock.arc.deployment.BeanDeploymentValidatorBuildItem;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.BeanContainerBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.scheduler.api.Scheduled;
import org.jboss.shamrock.scheduler.api.ScheduledExecution;
import org.jboss.shamrock.scheduler.api.Scheduleds;
import org.jboss.shamrock.scheduler.runtime.QuartzScheduler;
import org.jboss.shamrock.scheduler.runtime.ScheduledInvoker;
import org.jboss.shamrock.scheduler.runtime.SchedulerConfiguration;
import org.jboss.shamrock.scheduler.runtime.SchedulerDeploymentTemplate;
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
    static final DotName SCHEDULEDS_NAME = DotName.createSimple(Scheduleds.class.getName());

    static final Type SCHEDULED_EXECUTION_TYPE = Type.create(DotName.createSimple(ScheduledExecution.class.getName()), Kind.CLASS);

    static final String INVOKER_SUFFIX = "_ScheduledInvoker";

    private static final AtomicInteger INVOKER_INDEX = new AtomicInteger();

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
                    if (context.getTarget().asClass().annotations().containsKey(SCHEDULED_NAME) || context.getTarget().asClass().annotations().containsKey(SCHEDULEDS_NAME)) {
                        LOGGER.debugf("Found scheduled business methods on a class %s with no annotations - adding @Singleton", context.getTarget());
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
    BeanDeploymentValidatorBuildItem beanDeploymentValidator(BuildProducer<ScheduledBusinessMethodItem> scheduledBusinessMethods) {
        
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
                                AnnotationInstance scheduledsAnnotation = annotationStore.getAnnotation(method, SCHEDULEDS_NAME);
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
                                if (params.size() > 1 || (params.size() == 1 && !params.get(0).equals(SCHEDULED_EXECUTION_TYPE))) {
                                    throw new IllegalStateException(String.format("Invalid scheduled business method parameters %s [method: %s, bean:%s", params, method, bean));
                                }
                                if (!method.returnType().kind().equals(Type.Kind.VOID)) {
                                    throw new IllegalStateException(String.format("Scheduled business method must return void [method: %s, bean:%s", method.returnType(), method, bean));
                                }
                                scheduledBusinessMethods.produce(new ScheduledBusinessMethodItem(bean, method, schedules));
                                // TODO: validate cron and period expressions
                                LOGGER.debugf("Found scheduled business method %s declared on %s", method, bean);
                            }
                        }

                    }
                }
            }
        });
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(SchedulerDeploymentTemplate template, BeanContainerBuildItem beanContainer, List<ScheduledBusinessMethodItem> scheduledBusinessMethods,
            BuildProducer<GeneratedClassBuildItem> generatedResource, BuildProducer<ReflectiveClassBuildItem> reflectiveClass, BuildProducer<FeatureBuildItem> feature) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SCHEDULER));
        List<Map<String, Object>> scheduleConfigurations = new ArrayList<>();
        ProcessorClassOutput processorClassOutput = new ProcessorClassOutput(generatedResource);

        for (ScheduledBusinessMethodItem businessMethod : scheduledBusinessMethods) {
            Map<String, Object> config = new HashMap<>();
            String invokerClass = generateInvoker(businessMethod.getBean(), businessMethod.getMethod(), processorClassOutput);
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, invokerClass));
            config.put(SchedulerDeploymentTemplate.INVOKER_KEY, invokerClass);
            List<Map<String, Object>> schedules = new ArrayList<>();
            for (AnnotationInstance scheduled : businessMethod.getSchedules()) {
                Map<String, Object> scheduledConfig = new HashMap<>();
                for (AnnotationValue annotationValue : scheduled.values()) {
                    scheduledConfig.put(annotationValue.name(), getValue(annotationValue));
                }
                schedules.add(scheduledConfig);
            }
            config.put(SchedulerDeploymentTemplate.SCHEDULES_KEY, schedules);
            config.put(SchedulerDeploymentTemplate.DESC_KEY, businessMethod.getMethod().declaringClass() + "#" + businessMethod.getMethod().name());
            scheduleConfigurations.add(config);
        }
        template.registerSchedules(scheduleConfigurations, beanContainer.getValue());
    }

    private String generateInvoker(BeanInfo bean, MethodInfo method, ProcessorClassOutput processorClassOutput) {

        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = DotNames.simpleName(bean.getImplClazz().enclosingClass()) + "_" + DotNames.simpleName(bean.getImplClazz().name());
        } else {
            baseName = DotNames.simpleName(bean.getImplClazz().name());
        }
        String targetPackage = DotNames.packageName(bean.getImplClazz().name());
        String generatedName = targetPackage.replace('.', '/') + "/" + baseName + INVOKER_SUFFIX + INVOKER_INDEX.incrementAndGet();

        ClassCreator invokerCreator = ClassCreator.builder().classOutput(processorClassOutput).className(generatedName).interfaces(ScheduledInvoker.class)
                .build();

        MethodCreator invoke = invokerCreator.getMethodCreator("invoke", void.class, ScheduledExecution.class);
        // InjectableBean<Foo: bean = Arc.container().bean("1");
        // InstanceHandle<Foo> handle = Arc.container().instance(bean);
        // handle.get().ping();
        ResultHandle containerHandle = invoke.invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle beanHandle = invoke.invokeInterfaceMethod(MethodDescriptor.ofMethod(ArcContainer.class, "bean", InjectableBean.class, String.class),
                containerHandle, invoke.load(bean.getIdentifier()));
        ResultHandle instanceHandle = invoke.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, InjectableBean.class), containerHandle, beanHandle);
        ResultHandle beanInstanceHandle = invoke.invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);
        if (method.parameters().isEmpty()) {
            invoke.invokeVirtualMethod(MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class), beanInstanceHandle);
        } else {
            invoke.invokeVirtualMethod(MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class, ScheduledExecution.class),
                    beanInstanceHandle, invoke.getMethodParam(0));
        }
        // handle.destroy() - destroy dependent instance afterwards
        if (bean.getScope() == ScopeInfo.DEPENDENT) {
            invoke.invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "destroy", void.class), instanceHandle);
        }
        invoke.returnValue(null);

        invokerCreator.close();
        return generatedName.replace('/', '.');
    }

    private Object getValue(AnnotationValue annotationValue) {
        switch (annotationValue.kind()) {
            case STRING:
                return annotationValue.asString();
            case LONG:
                return annotationValue.asLong();
            case ENUM:
                return annotationValue.asEnum();
            default:
                throw new IllegalArgumentException("Unsupported annotation value: " + annotationValue);
        }
    }

    static final class ProcessorClassOutput implements ClassOutput {
        private final BuildProducer<GeneratedClassBuildItem> producer;

        ProcessorClassOutput(BuildProducer<GeneratedClassBuildItem> producer) {
            this.producer = producer;
        }

        @Override
        public void write(final String name, final byte[] data) {
            producer.produce(new GeneratedClassBuildItem(true, name, data));
        }

    }

}
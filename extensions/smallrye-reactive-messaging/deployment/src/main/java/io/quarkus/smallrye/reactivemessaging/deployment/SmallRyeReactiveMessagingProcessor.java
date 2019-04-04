/*
 * Copyright 2019 Red Hat, Inc.
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
package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDeploymentValidatorBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingLifecycle;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingTemplate;
import io.smallrye.reactive.messaging.annotations.Emitter;
import io.smallrye.reactive.messaging.annotations.Stream;

/**
 *
 * @author Martin Kouba
 */
public class SmallRyeReactiveMessagingProcessor {

    private static final Logger LOGGER = Logger.getLogger("io.quarkus.smallrye-reactive-messaging.deployment.processor");

    static final DotName NAME_INCOMING = DotName.createSimple(Incoming.class.getName());
    static final DotName NAME_OUTGOING = DotName.createSimple(Outgoing.class.getName());
    static final DotName NAME_STREAM = DotName.createSimple(Stream.class.getName());
    static final DotName NAME_EMITTER = DotName.createSimple(Emitter.class.getName());

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(SmallRyeReactiveMessagingLifecycle.class);
    }

    @BuildStep
    BeanDeploymentValidatorBuildItem beanDeploymentValidator(BuildProducer<MediatorBuildItem> mediatorMethods,
            BuildProducer<EmitterBuildItem> emitterFields,
            BuildProducer<FeatureBuildItem> feature) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_REACTIVE_MESSAGING));

        return new BeanDeploymentValidatorBuildItem(new BeanDeploymentValidator() {

            @Override
            public void validate(ValidationContext validationContext) {

                AnnotationStore annotationStore = validationContext.get(Key.ANNOTATION_STORE);

                // We need to collect all business methods annotated with @Incoming/@Outgoing first
                for (BeanInfo bean : validationContext.get(Key.BEANS)) {
                    if (bean.isClassBean()) {
                        // TODO: add support for inherited business methods
                        ClassInfo ci = bean.getTarget()
                                .orElseThrow(() -> new IllegalStateException("Target expected")).asClass();
                        for (MethodInfo method : ci.methods()) {
                            if (annotationStore.hasAnnotation(method, NAME_INCOMING)
                                    || annotationStore.hasAnnotation(method, NAME_OUTGOING)) {
                                // TODO: validate method params and return type?
                                mediatorMethods.produce(new MediatorBuildItem(bean, method));
                                LOGGER.debugf("Found mediator business method %s declared on %s", method, bean);
                            }
                        }

                        for (FieldInfo field : ci.fields()) {
                            if (annotationStore.hasAnnotation(field, NAME_STREAM)) {
                                if (field.type().name().equals(NAME_EMITTER)) {
                                    String name = annotationStore.getAnnotation(field, NAME_STREAM).value().asString();
                                    LOGGER.debugf("Emitter field '%s'  detected, stream name: '%s'", field.name(), name);
                                    emitterFields.produce(new EmitterBuildItem(name));
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @BuildStep
    public List<UnremovableBeanBuildItem> removalExclusions() {
        return Arrays.asList(new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(NAME_INCOMING)),
                new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(NAME_OUTGOING)));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(SmallRyeReactiveMessagingTemplate template, BeanContainerBuildItem beanContainer,
            List<MediatorBuildItem> mediatorMethods,
            List<EmitterBuildItem> emitterFields,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        /*
         * IMPLEMENTATION NOTE/FUTURE IMPROVEMENTS: It would be possible to replace the reflection completely and use Jandex and
         * generated
         * io.smallrye.reactive.messaging.Invoker instead. However, we would have to mirror the logic from
         * io.smallrye.reactive.messaging.MediatorConfiguration
         * too.
         */
        Map<String, String> beanClassToBeanId = new HashMap<>();
        for (MediatorBuildItem mediatorMethod : mediatorMethods) {
            String beanClass = mediatorMethod.getBean()
                    .getBeanClass()
                    .toString();
            if (!beanClassToBeanId.containsKey(beanClass)) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, beanClass));
                beanClassToBeanId.put(beanClass, mediatorMethod.getBean()
                        .getIdentifier());
            }
        }
        template.registerMediators(beanClassToBeanId, beanContainer.getValue(),
                emitterFields.stream().map(EmitterBuildItem::getName).collect(Collectors.toList()));
    }

}

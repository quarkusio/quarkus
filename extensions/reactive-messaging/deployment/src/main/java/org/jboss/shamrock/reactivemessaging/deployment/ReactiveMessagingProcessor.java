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
package org.jboss.shamrock.reactivemessaging.deployment;

import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.jboss.protean.arc.processor.AnnotationStore;
import org.jboss.protean.arc.processor.BeanDeploymentValidator;
import org.jboss.protean.arc.processor.BeanInfo;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.arc.deployment.BeanDeploymentValidatorBuildItem;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.BeanContainerBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.reactivemessaging.runtime.ReactiveMessagingLifecycle;
import org.jboss.shamrock.reactivemessaging.runtime.ReactiveMessagingTemplate;

/**
 *
 * @author Martin Kouba
 */
public class ReactiveMessagingProcessor {

    private static final Logger LOGGER = Logger.getLogger("org.jboss.shamrock.scheduler.deployment.processor");

    static final DotName NAME_INCOMING = DotName.createSimple(Incoming.class.getName());
    static final DotName NAME_OUTGOING = DotName.createSimple(Outgoing.class.getName());

    @BuildStep
    List<AdditionalBeanBuildItem> beans() {
        List<AdditionalBeanBuildItem> beans = new ArrayList<>();
        beans.add(new AdditionalBeanBuildItem(ReactiveMessagingLifecycle.class));
        return beans;
    }

    @BuildStep
    BeanDeploymentValidatorBuildItem beanDeploymentValidator(BuildProducer<MediatorBuildItem> mediatorMethods, BuildProducer<FeatureBuildItem> feature) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.MP_REACTIVE_MESSAGING));

        return new BeanDeploymentValidatorBuildItem(new BeanDeploymentValidator() {

            @Override
            public void validate(ValidationContext validationContext) {

                AnnotationStore annotationStore = validationContext.get(Key.ANNOTATION_STORE);

                // We need to collect all business methods annotated with @Incoming/@Outgoing first
                for (BeanInfo bean : validationContext.get(Key.BEANS)) {
                    if (bean.isClassBean()) {
                        // TODO: add support for inherited business methods
                        for (MethodInfo method : bean.getTarget()
                                .get()
                                .asClass()
                                .methods()) {
                            if (annotationStore.hasAnnotation(method, NAME_INCOMING) || annotationStore.hasAnnotation(method, NAME_OUTGOING)) {
                                // TODO: validate method params and return type?
                                mediatorMethods.produce(new MediatorBuildItem(bean, method));
                                LOGGER.debugf("Found mediator business method %s declared on %s", method, bean);
                            }
                        }
                    }
                }
            }
        });
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(ReactiveMessagingTemplate template, BeanContainerBuildItem beanContainer, List<MediatorBuildItem> mediatorMethods,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        /*
         * IMPLEMENTATION NOTE/FUTURE IMPROVEMENTS: It would be possible to replace the reflection completely and use Jandex and generated
         * io.smallrye.reactive.messaging.Invoker instead. However, we would have to mirror the logic from io.smallrye.reactive.messaging.MediatorConfiguration
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
        template.registerMediators(beanClassToBeanId, beanContainer.getValue());
    }

}
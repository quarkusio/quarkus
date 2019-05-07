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

package io.quarkus.smallrye.context.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationProvider;
import io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationTemplate;

/**
 * The deployment processor for MP-CP applications
 */
class SmallRyeContextPropagationProcessor {
    private static final Logger log = Logger.getLogger(SmallRyeContextPropagationProcessor.class.getName());

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.builder().addBeanClass(SmallRyeContextPropagationProvider.class).build();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void buildStatic(SmallRyeContextPropagationTemplate template)
            throws ClassNotFoundException, IOException {
        List<ThreadContextProvider> discoveredProviders = new ArrayList<>();
        List<ContextManagerExtension> discoveredExtensions = new ArrayList<>();
        for (Class<?> provider : ServiceUtil.classesNamedIn(SmallRyeContextPropagationTemplate.class.getClassLoader(),
                "META-INF/services/" + ThreadContextProvider.class.getName())) {
            try {
                discoveredProviders.add((ThreadContextProvider) provider.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to instantiate declared ThreadContextProvider class: " + provider.getName(),
                        e);
            }
        }
        for (Class<?> extension : ServiceUtil.classesNamedIn(SmallRyeContextPropagationTemplate.class.getClassLoader(),
                "META-INF/services/" + ContextManagerExtension.class.getName())) {
            try {
                discoveredExtensions.add((ContextManagerExtension) extension.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to instantiate declared ThreadContextProvider class: " + extension.getName(),
                        e);
            }
        }

        template.configureStaticInit(discoveredProviders, discoveredExtensions);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void build(SmallRyeContextPropagationTemplate template,
            BeanContainerBuildItem beanContainer,
            ExecutorBuildItem executorBuildItem) {
        template.configureRuntime(beanContainer.getValue(), executorBuildItem.getExecutorProxy());
    }
}

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

package org.jboss.shamrock.weld.deployment;

import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.BeanArchiveIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.BeanContainerBuildItem;
import org.jboss.shamrock.deployment.builditem.InjectionProviderBuildItem;
import org.jboss.shamrock.deployment.builditem.ShutdownContextBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.cdi.BeanContainerListenerBuildItem;
import org.jboss.shamrock.deployment.cdi.CdiExtensionBuildItem;
import org.jboss.shamrock.deployment.cdi.GeneratedBeanBuildItem;
import org.jboss.shamrock.deployment.recording.RecorderContext;
import org.jboss.shamrock.runtime.cdi.BeanContainer;
import org.jboss.shamrock.weld.runtime.WeldDeploymentTemplate;

import io.smallrye.config.inject.ConfigProducer;

public class WeldAnnotationProcessor {

    @Inject
    BeanArchiveIndexBuildItem beanArchiveIndex;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    List<AdditionalBeanBuildItem> additionalBeans;

    @Record(STATIC_INIT)
    @BuildStep(providesCapabilities = Capabilities.CDI_WELD, applicationArchiveMarkers = {"META-INF/beans.xml", "META-INF/services/javax.enterprise.inject.spi.Extension"})
    public BeanContainerBuildItem build(WeldDeploymentTemplate template, RecorderContext recorder,
                                        BuildProducer<InjectionProviderBuildItem> injectionProvider,
                                        List<BeanContainerListenerBuildItem> beanConfig,
                                        List<GeneratedBeanBuildItem> generatedBeans,
                                        List<CdiExtensionBuildItem> extensions,
                                        ShutdownContextBuildItem shutdown) throws Exception {
        IndexView index = beanArchiveIndex.getIndex();
        List<String> additionalBeans = new ArrayList<>();
        for (AdditionalBeanBuildItem i : this.additionalBeans) {
            additionalBeans.addAll(i.getBeanNames());
        }
        //make config injectable
        additionalBeans.add(ConfigProducer.class.getName());
        SeContainerInitializer init = template.createWeld();
        for (ClassInfo cl : index.getKnownClasses()) {
            String name = cl.name().toString();
            template.addClass(init, recorder.classProxy(name));
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, name));
        }
        for (String clazz : additionalBeans) {
            template.addClass(init, recorder.classProxy(clazz));
        }
        for (GeneratedBeanBuildItem clazz : generatedBeans) {
            template.addClass(init, recorder.classProxy(clazz.getName()));
        }
        for (CdiExtensionBuildItem extensionClazz : extensions) {
            template.addExtension(init, recorder.classProxy(extensionClazz.getName()));
        }
        SeContainer weld = template.doBoot(shutdown, init);
        BeanContainer container = template.initBeanContainer(weld, beanConfig.stream().map(BeanContainerListenerBuildItem::getBeanContainerListener).collect(Collectors.toList()));
        injectionProvider.produce(new InjectionProviderBuildItem(template.setupInjection(weld)));
        return new BeanContainerBuildItem(container);
    }
}

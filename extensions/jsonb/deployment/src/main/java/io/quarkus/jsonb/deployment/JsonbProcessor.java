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
package io.quarkus.jsonb.deployment;

import java.util.function.Predicate;

import javax.json.bind.adapter.JsonbAdapter;

import org.eclipse.yasson.JsonBindingProvider;
import org.eclipse.yasson.spi.JsonbComponentInstanceCreator;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;
import io.quarkus.jsonb.QuarkusJsonbComponentInstanceCreator;

public class JsonbProcessor {

    static final DotName JSONB_ADAPTER_NAME = DotName.createSimple(JsonbAdapter.class.getName());

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<SubstrateResourceBundleBuildItem> resourceBundle,
            BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                JsonBindingProvider.class.getName()));

        resourceBundle.produce(new SubstrateResourceBundleBuildItem("yasson-messages"));

        serviceProvider.produce(new ServiceProviderBuildItem(JsonbComponentInstanceCreator.class.getName(),
                QuarkusJsonbComponentInstanceCreator.class.getName()));
    }

    @BuildStep
    void unremovableJsonbAdapters(BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BeanArchiveIndexBuildItem beanArchiveIndex) {
        unremovableBeans.produce(new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {

            @Override
            public boolean test(BeanInfo bean) {
                return bean.isClassBean() && bean.hasType(JSONB_ADAPTER_NAME);
            }
        }));
    }

}

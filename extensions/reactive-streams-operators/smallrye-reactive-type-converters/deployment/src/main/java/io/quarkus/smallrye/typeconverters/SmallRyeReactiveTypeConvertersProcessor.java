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

package io.quarkus.smallrye.typeconverters;

import java.util.Collection;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

/**
 * Searches for implementations of the {@link ReactiveTypeConverter} class and register them as SPI. So the result depends
 * of the implementation added by the user in the build classpath (Maven dependencies).
 *
 * Note that if none are found, nothing is added - so declaring this augmentation is quite useless in this case.
 */
public class SmallRyeReactiveTypeConvertersProcessor {

    private static final DotName REACTIVE_TYPE_CONVERTER = DotName.createSimple(ReactiveTypeConverter.class.getName());

    @BuildStep
    public void build(BuildProducer<ServiceProviderBuildItem> serviceProvider, BuildProducer<FeatureBuildItem> feature,
            CombinedIndexBuildItem indexBuildItem) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_REACTIVE_TYPE_CONVERTERS));
        Collection<ClassInfo> implementors = indexBuildItem.getIndex().getAllKnownImplementors(REACTIVE_TYPE_CONVERTER);

        implementors.forEach(info -> serviceProvider
                .produce(new ServiceProviderBuildItem(REACTIVE_TYPE_CONVERTER.toString(), info.toString())));
    }

}

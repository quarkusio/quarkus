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

package org.jboss.shamrock.rsops;

import io.smallrye.reactive.streams.Engine;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ServiceProviderBuildItem;

public class ReactiveStreamsOperatorsProcessor {

    @BuildStep
    public void build(BuildProducer<ServiceProviderBuildItem> serviceProvider, BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.MP_REACTIVE_OPERATORS));
        serviceProvider.produce(new ServiceProviderBuildItem(ReactiveStreamsEngine.class.getName(), Engine.class.getName()));
        serviceProvider.produce(new ServiceProviderBuildItem(ReactiveStreamsFactory.class.getName(), ReactiveStreamsFactoryImpl.class.getName()));
    }

}

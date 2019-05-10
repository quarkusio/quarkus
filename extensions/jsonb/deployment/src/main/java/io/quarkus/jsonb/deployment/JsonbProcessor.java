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

import org.eclipse.yasson.JsonBindingProvider;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;

public class JsonbProcessor {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<SubstrateResourceBundleBuildItem> resourceBundle) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                JsonBindingProvider.class.getName()));

        resourceBundle.produce(new SubstrateResourceBundleBuildItem("yasson-messages"));
    }
}

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

package org.jboss.shamrock.deployment.steps;

import java.util.List;
import java.util.Map;

import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateConfigBuildItem;

//TODO: this should go away, once we decide on which one of the API's we want
class SubstrateConfigBuildStep {

    @BuildStep
    void build(List<SubstrateConfigBuildItem> substrateConfigBuildItems,
               BuildProducer<SubstrateProxyDefinitionBuildItem> proxy,
               BuildProducer<SubstrateResourceBundleBuildItem> resourceBundle,
               BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit,
               BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReinit,
               BuildProducer<SubstrateSystemPropertyBuildItem> nativeImage) {
        for (SubstrateConfigBuildItem substrateConfigBuildItem : substrateConfigBuildItems) {
            for (String i : substrateConfigBuildItem.getRuntimeInitializedClasses()) {
                runtimeInit.produce(new RuntimeInitializedClassBuildItem(i));
            }
            for (String i : substrateConfigBuildItem.getRuntimeReinitializedClasses()) {
                runtimeReinit.produce(new RuntimeReinitializedClassBuildItem(i));
            }
            for (Map.Entry<String, String> e : substrateConfigBuildItem.getNativeImageSystemProperties().entrySet()) {
                nativeImage.produce(new SubstrateSystemPropertyBuildItem(e.getKey(), e.getValue()));
            }
            for (String i : substrateConfigBuildItem.getResourceBundles()) {
                resourceBundle.produce(new SubstrateResourceBundleBuildItem(i));
            }
            for (List<String> i : substrateConfigBuildItem.getProxyDefinitions()) {
                proxy.produce(new SubstrateProxyDefinitionBuildItem(i));
            }
        }
    }
}

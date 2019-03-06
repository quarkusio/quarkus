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

package io.quarkus.jaeger.deployment;

import javax.inject.Inject;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jaeger.runtime.JaegerConfig;
import io.quarkus.jaeger.runtime.JaegerDeploymentTemplate;

public class JaegerProcessor {

    @Inject
    BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport;

    /**
     * The jaeger configuration
     */
    JaegerConfig jaeger;

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupTracer(JaegerDeploymentTemplate jdt) {

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.JAEGER));

        jdt.registerTracer(jaeger);
    }

    @BuildStep
    public void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.JAEGER));
    }

}

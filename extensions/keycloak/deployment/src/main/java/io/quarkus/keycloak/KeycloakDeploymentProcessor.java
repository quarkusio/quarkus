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
package io.quarkus.keycloak;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class KeycloakDeploymentProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureKeycloakAdapter(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.KEYCLOAK));
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.KEYCLOAK));

        // register producers for injection and configuration of keycloak components
        beans.produce(new AdditionalBeanBuildItem(KeycloakSecurityContextProducer.class));
        beans.produce(new AdditionalBeanBuildItem(QuarkusKeycloakConfigResolver.class));
    }
}

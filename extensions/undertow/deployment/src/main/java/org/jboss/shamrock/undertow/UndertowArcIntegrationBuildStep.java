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

package io.quarkus.undertow;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.undertow.runtime.UndertowDeploymentTemplate;

public class UndertowArcIntegrationBuildStep {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    ServletExtensionBuildItem integrateRequestContext(BeanContainerBuildItem beanContainerBuildItem,
            UndertowDeploymentTemplate template) {
        return new ServletExtensionBuildItem(template.setupRequestScope(beanContainerBuildItem.getValue()));
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> annotations) {
        annotations.produce(new BeanDefiningAnnotationBuildItem(UndertowBuildStep.WEB_FILTER));
        annotations.produce(new BeanDefiningAnnotationBuildItem(UndertowBuildStep.WEB_SERVLET));
        annotations.produce(new BeanDefiningAnnotationBuildItem(UndertowBuildStep.WEB_LISTENER));
    }
}

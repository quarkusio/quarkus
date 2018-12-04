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

package org.jboss.shamrock.deployment;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jboss.jandex.DotName;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.cdi.BeanDefiningAnnotationBuildItem;

import io.smallrye.config.inject.ConfigProducer;

public class CoreBuildStep {

    @Inject
    Capabilities capabilities;

    @BuildStep
    List<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations() {
        if (capabilities.isCapabilityPresent(Capabilities.CDI_ARC)) {
            // Enables injection into shamrock unit tests
            // TODO: we should only add this when running the tests
            return Collections.singletonList(new BeanDefiningAnnotationBuildItem(DotName.createSimple("org.junit.runner.RunWith")));
        }
        return Collections.emptyList();
    }

    @BuildStep
    List<AdditionalBeanBuildItem> beans() {
        if (capabilities.isCapabilityPresent(Capabilities.CDI_ARC)) {
            return Collections.singletonList(new AdditionalBeanBuildItem(ConfigProducer.class.getName()));
        }
        return Collections.emptyList();
    }

}

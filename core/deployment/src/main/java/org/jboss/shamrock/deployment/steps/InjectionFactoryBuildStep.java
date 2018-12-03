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

import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.ExecutionTime;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.builditem.InjectionFactoryBuildItem;
import org.jboss.shamrock.deployment.builditem.InjectionProviderBuildItem;
import org.jboss.shamrock.runtime.DefaultInjectionTemplate;
import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.InjectionFactoryTemplate;

public class InjectionFactoryBuildStep {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    InjectionFactoryBuildItem factory(List<InjectionProviderBuildItem> providers, DefaultInjectionTemplate template,
                                      InjectionFactoryTemplate ifTemplate) {
        if (providers.isEmpty()) {
            InjectionFactory factory = template.defaultFactory();
            ifTemplate.setFactory(factory);
            return new InjectionFactoryBuildItem(factory);
        } else if (providers.size() != 1) {
            throw new RuntimeException("At most a single Injection provider can be registered. Make sure you have not included multiple dependency injection containers in your project (e.g. Weld and Arc) " + providers);
        }
        InjectionFactory factory = providers.get(0).getFactory();
        ifTemplate.setFactory(factory);
        return new InjectionFactoryBuildItem(factory);
    }


}

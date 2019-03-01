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

package io.quarkus.agroal.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Template;

@Template
public class AgroalTemplate {

    public static final String DEFAULT_DATASOURCE_NAME = "<default>";

    public BeanContainerListener addDataSource(
            Class<? extends AbstractDataSourceProducer> dataSourceProducerClass,
            AgroalBuildTimeConfig agroalBuildTimeConfig,
            boolean disableSslSupport) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                AbstractDataSourceProducer producer = beanContainer.instance(dataSourceProducerClass);

                producer.setBuildTimeConfig(agroalBuildTimeConfig);

                if (disableSslSupport) {
                    producer.disableSslSupport();
                }
            }
        };
    }

    public void configureRuntimeProperties(AgroalRuntimeConfig agroalRuntimeConfig) {
        // TODO @dmlloyd
        // Same here, the map is entirely empty (obviously, I didn't expect the values
        // that were not properly injected but at least the config objects present in
        // the map)
        // The elements from the default datasource are there
        Arc.container().instance(AbstractDataSourceProducer.class).get().setRuntimeConfig(agroalRuntimeConfig);
    }
}

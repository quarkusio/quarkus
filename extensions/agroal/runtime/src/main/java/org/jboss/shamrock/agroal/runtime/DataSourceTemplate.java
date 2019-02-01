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

package org.jboss.shamrock.agroal.runtime;

import org.jboss.shamrock.arc.runtime.BeanContainer;
import org.jboss.shamrock.arc.runtime.BeanContainerListener;
import org.jboss.shamrock.runtime.annotations.Template;

@Template
public class DataSourceTemplate {

    public BeanContainerListener addDatasource(DataSourceConfig config) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                DataSourceProducer producer = beanContainer.instance(DataSourceProducer.class);
                try {
                    producer.setDriver(Class.forName(config.driver.get(), true, Thread.currentThread().getContextClassLoader()));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                producer.setUrl(config.url.get());
                if (config.username.isPresent()) {
                    producer.setUserName(config.username.get());
                }
                if (config.password.isPresent()) {
                    producer.setPassword(config.password.get());
                }
                producer.setMinSize(config.minSize);
                producer.setMaxSize(config.maxSize);
            }
        };
    }

}

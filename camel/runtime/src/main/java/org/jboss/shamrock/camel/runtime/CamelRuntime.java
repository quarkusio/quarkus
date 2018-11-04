/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.camel.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.main.MainSupport;

public abstract class CamelRuntime extends RouteBuilder {

    private Main main = new Main();
    private SimpleLazyRegistry registry;
    private Properties properties;

    public CamelRuntime() {
    }

    public void bind(String name, Object object) {
        registry.put(name, object);
    }

    public void init() throws Exception {
        if (main.getRouteBuilders().isEmpty()) {
            main.init();
            main.getRouteBuilders().add(this);
            main.start();
        }
    }

    public void run() throws Exception {
        init();
        main.run();
    }

    protected DefaultCamelContext createContext() {
        return new FastCamelContext();
    }

    public void setRegistry(SimpleLazyRegistry registry) {
        this.registry = registry;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    class Main extends MainSupport {
        public Main() {
            options.removeIf(o -> "-r".equals(o.getAbbreviation()));
        }

        @Override
        protected void doInit() {
            try {
                disableHangupSupport();

                DefaultCamelContext context = createContext();
                context.setRegistry(registry);
                context.setAutoStartup(false);

                PropertiesComponent props = new PropertiesComponent();
                props.setInitialProperties(properties);
                context.addComponent("properties", props);

                context.start();
                setContext(context);
            } catch (Exception e) {
                throw RuntimeCamelException.wrapRuntimeCamelException(e);
            }
        }

        @Override
        protected void doStop() throws Exception {
            getContext().stop();
            completed();
        }

        @Override
        protected void doStart() throws Exception {
            postProcessContext();
            for (Route route : getContext().getRoutes()) {
                getContext().getRouteController().startRoute(route.getId());
            }
        }

        @Override
        protected ProducerTemplate findOrCreateCamelTemplate() {
            return getContext().createProducerTemplate();
        }

        @Override
        protected Map<String, CamelContext> getCamelContextMap() {
            return Collections.singletonMap("camel-1", getContext());
        }
    }

}

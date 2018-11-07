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

import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RouteDefinition;

public abstract class CamelRuntime extends RouteBuilder {

    private SimpleLazyRegistry registry;
    private Properties properties;

    public void bind(String name, Class<?> type, Object object) {
        registry.bind(name, type, object);
    }

    public void init() throws Exception {
        DefaultCamelContext context = createContext();
        context.setRegistry(registry);
        context.setAutoStartup(false);

        context.getModelJAXBContextFactory().newJAXBContext();

        PropertiesComponent props = new PropertiesComponent();
        props.setInitialProperties(properties);
        context.addComponent("properties", props);

        context.addRoutes(this);
        context.start();
        setContext(context);
    }

    public void run() throws Exception {
        CamelContext context = getContext();
        log.info("Apache Camel {} (CamelContext: {}) is starting", context.getVersion(), context.getName());
        if (Boolean.parseBoolean(properties.getProperty("camel.dump", "false"))) {
            dumpRoutes();
        }
        for (Route route : getContext().getRoutes()) {
            getContext().getRouteController().startRoute(route.getId());
        }
        // start a non-daemon thread that waits forever
        new Thread(() -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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

    protected void dumpRoutes() {
        long t0 = System.nanoTime();
        try {
            for (Route route : getContext().getRoutes()) {
                RouteDefinition def = (RouteDefinition) route.getRouteContext().getRoute();
                System.err.println("Route: " + def);
                String xml = ModelHelper.dumpModelAsXml(getContext(), def);
                System.err.println("Xml: " + xml);
            }
        } catch (Throwable t) {
            // ignore
            System.err.println("Error dumping route xml: " + t.getClass().getName() + ": " + t.getMessage());
            for (StackTraceElement e : t.getStackTrace()) {
                System.err.println("    " + e.getClassName() + " " + e.getMethodName() + " " + e.getLineNumber());
            }
        }
        long t1 = System.nanoTime();
        System.err.println("Dump routes: " + (t1 - t0) + " ns");
    }

}

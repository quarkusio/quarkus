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

package io.quarkus.camel.core.runtime;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.AbstractCamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;

public class CamelRuntime extends ServiceSupport {

    public static final String PFX_CAMEL = "camel.";
    public static final String PFX_CAMEL_PROPERTIES = PFX_CAMEL + "properties.";
    public static final String PFX_CAMEL_CONTEXT = PFX_CAMEL + "context.";

    public static final String PROP_CAMEL_RUNTIME = PFX_CAMEL + "runtime";
    public static final String PROP_CAMEL_CONF = PFX_CAMEL + "conf";
    public static final String PROP_CAMEL_CONFD = PFX_CAMEL + "confd";
    public static final String PROP_CAMEL_DUMP = PFX_CAMEL + "dump";
    public static final String PROP_CAMEL_DEFER = PFX_CAMEL + "defer";

    protected RuntimeRegistry registry;
    protected Properties properties;
    protected AbstractCamelContext context;
    protected List<RoutesBuilder> builders;
    protected PropertiesComponent propertiesComponent;

    public void bind(String name, Object object) {
        registry.bind(name, object);
    }

    public void bind(String name, Class<?> type, Object object) {
        registry.bind(name, type, object);
    }

    public void doInit() {
        try {
            AbstractCamelContext context = createContext();
            context.setRegistry(registry);
            context.setAutoStartup(false);
            this.context = context;

            // Configure the camel context using properties in the form:
            //
            //     camel.context.${name} = ${value}
            //
            RuntimeSupport.bindProperties(properties, context, PFX_CAMEL_CONTEXT);

            context.getModelJAXBContextFactory().newJAXBContext();

            propertiesComponent = new PropertiesComponent();
            propertiesComponent.setInitialProperties(properties);
            RuntimeSupport.bindProperties(properties, propertiesComponent, PFX_CAMEL_PROPERTIES);
            context.addComponent("properties", propertiesComponent);

            loadRoutesFromBuilders(true);

            context.start();
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }
    }

    public void doStart() throws Exception {
        log.info("Apache Camel {} (CamelContext: {}) is starting", context.getVersion(), context.getName());

        String conf = getProperty(PROP_CAMEL_CONF);
        String confd = getProperty(PROP_CAMEL_CONFD);
        log.info("confPath: {}", conf);
        log.info("confDPath: {}", confd);
        RuntimeSupport.loadConfigSources(properties, conf, confd);

        loadRoutesFromBuilders(false);
        loadRoutes();

        if (Boolean.parseBoolean(getProperty(PROP_CAMEL_DUMP))) {
            dumpRoutes();
        }

        for (Route route : getContext().getRoutes()) {
            getContext().getRouteController().startRoute(route.getId());
        }
    }

    @Override
    protected void doStop() throws Exception {
        context.shutdown();
    }

    protected void loadRoutesFromBuilders(boolean initPhase) throws Exception {
        if (builders != null && !builders.isEmpty()) {
            boolean defer = Boolean.parseBoolean(getProperty(PROP_CAMEL_DEFER));
            if (defer ^ initPhase) {
                for (RoutesBuilder b : builders) {
                    if (b instanceof RouteBuilderExt) {
                        ((RouteBuilderExt) b).setRegistry(registry);
                    }
                    context.addRoutes(b);
                }
            }
        }
    }

    protected void loadRoutes() throws Exception {
        String routesUri = getProperty("camel.routesUri");
        log.info("routesUri: {}", routesUri);
        if (ObjectHelper.isNotEmpty(routesUri)) {
            try (InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(getContext(), routesUri)) {
                context.addRouteDefinitions(context.loadRoutesDefinition(is).getRoutes());
            }
        }
    }

    protected String getProperty(String name) throws Exception {
        return propertiesComponent.parseUri(propertiesComponent.getPrefixToken() + name + propertiesComponent.getSuffixToken());
    }

    protected DefaultCamelContext createContext() {
        return new FastCamelContext();
    }

    public void setRegistry(RuntimeRegistry registry) {
        this.registry = registry;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setBuilders(List<RoutesBuilder> builders) {
        this.builders = builders;
    }

    public CamelContext getContext() {
        return context;
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

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
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;
import org.graalvm.nativeimage.ImageInfo;

public class CamelRuntime extends ServiceSupport {

    public static final String PFX_CAMEL = "camel.";
    public static final String PFX_CAMEL_PROPERTIES = PFX_CAMEL + "component.properties.";
    public static final String PFX_CAMEL_CONTEXT = PFX_CAMEL + "context.";

    public static final String PROP_CAMEL_RUNTIME = PFX_CAMEL + "runtime";
    public static final String PROP_CAMEL_ROUTES = PFX_CAMEL + "routes.";
    public static final String PROP_CAMEL_ROUTES_DUMP = PROP_CAMEL_ROUTES + "dump";
    public static final String PROP_CAMEL_ROUTES_LOCATIONS = PROP_CAMEL_ROUTES + "locations";

    protected RuntimeRegistry registry;
    protected Properties properties;
    protected AbstractCamelContext context;
    protected List<RoutesBuilder> builders;

    public void bind(String name, Object object) {
        registry.bind(name, object);
    }

    public void bind(String name, Class<?> type, Object object) {
        registry.bind(name, type, object);
    }

    public void doInit() {
        try {
            this.context = createContext();
            this.context.setRegistry(registry);

            // Configure the camel context using properties in the form:
            //
            //     camel.context.${name} = ${value}
            //
            RuntimeSupport.bindProperties(properties, context, PFX_CAMEL_CONTEXT);

            context.setLoadTypeConverters(false);

            // The creation of the JAXB context is very time consuming, so always prepare it
            // when running in native mode, but lazy create it in java mode so that we don't
            // waste time if using java routes
            if (ImageInfo.inImageBuildtimeCode()) {
                context.getModelJAXBContextFactory().newJAXBContext();
            }
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }
    }

    public void doStart() throws Exception {
        log.info("Apache Camel {} (CamelContext: {}) is starting", context.getVersion(), context.getName());

        PropertiesComponent pc = createPropertiesComponent(properties);
        RuntimeSupport.bindProperties(pc.getInitialProperties(), pc, PFX_CAMEL_PROPERTIES);
        context.addComponent("properties", pc);

        configureContext(context);
        loadRoutes(context);

        context.start();

        if (Boolean.parseBoolean(getProperty(PROP_CAMEL_ROUTES_DUMP))) {
            dumpRoutes();
        }
    }

    @Override
    protected void doStop() throws Exception {
        context.shutdown();
    }

    protected void loadRoutes(CamelContext context) throws Exception {
        for (RoutesBuilder b : builders) {
            if (b instanceof RouteBuilderExt) {
                ((RouteBuilderExt) b).setRegistry(registry);
            }
            context.addRoutes(b);
        }

        String routesUri = getProperty(PROP_CAMEL_ROUTES_LOCATIONS);
        if (ObjectHelper.isNotEmpty(routesUri)) {
            log.info("routesUri: {}", routesUri);

            ModelCamelContext mcc = context.adapt(ModelCamelContext.class);

            try (InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(getContext(), routesUri)) {
                mcc.addRouteDefinitions(mcc.loadRoutesDefinition(is).getRoutes());
            }
        }
    }

    protected String getProperty(String name) throws Exception {
        return context.resolvePropertyPlaceholders(context.getPropertyPrefixToken() + name + context.getPropertySuffixToken());
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

    protected PropertiesComponent createPropertiesComponent(Properties initialPoperties) {
        PropertiesComponent pc = new PropertiesComponent();
        pc.setInitialProperties(initialPoperties);

        RuntimeSupport.bindProperties(properties, pc, PFX_CAMEL_PROPERTIES);

        return pc;
    }

    protected void configureContext(CamelContext context) {
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

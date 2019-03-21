package io.quarkus.camel.core.runtime.support;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.ShutdownableService;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;
import org.graalvm.nativeimage.ImageInfo;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.camel.core.runtime.CamelConfig.BuildTime;
import io.quarkus.camel.core.runtime.CamelConfig.Runtime;
import io.quarkus.camel.core.runtime.CamelRuntime;
import io.quarkus.camel.core.runtime.InitializedEvent;
import io.quarkus.camel.core.runtime.InitializingEvent;
import io.quarkus.camel.core.runtime.StartedEvent;
import io.quarkus.camel.core.runtime.StartingEvent;
import io.quarkus.camel.core.runtime.StoppedEvent;
import io.quarkus.camel.core.runtime.StoppingEvent;

public class FastCamelRuntime extends ServiceSupport implements CamelRuntime {

    protected CamelContext context;
    protected BeanContainer beanContainer;
    protected Registry registry;
    protected Properties properties;
    protected List<RoutesBuilder> builders;
    protected BuildTime buildTimeConfig;
    protected Runtime runtimeConfig;

    @Override
    public void init(BuildTime buildTimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        if (!buildTimeConfig.deferInitPhase) {
            init();
        }
    }

    @Override
    public void start(Runtime runtimeConfig) throws Exception {
        this.runtimeConfig = runtimeConfig;
        start();
    }

    public void doInit() {
        try {
            this.context = createContext();

            StringWriter sw = new StringWriter();
            this.properties.store(sw, "");
            log.warn("Properties: " + sw.toString());

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
                context.adapt(ModelCamelContext.class).getModelJAXBContextFactory().newJAXBContext();
            }

            PropertiesComponent pc = createPropertiesComponent(properties);
            RuntimeSupport.bindProperties(pc.getInitialProperties(), pc, PFX_CAMEL_PROPERTIES);
            context.addComponent("properties", pc);

            fireEvent(InitializingEvent.class, new InitializingEvent());
            this.context.getTypeConverterRegistry().setInjector(this.context.getInjector());
            this.context.init();
            fireEvent(InitializedEvent.class, new InitializedEvent());

            loadRoutes(context);
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }
    }

    public void doStart() throws Exception {
        log.info("Apache Camel {} (CamelContext: {}) is starting", context.getVersion(), context.getName());

        fireEvent(StartingEvent.class, new StartingEvent());
        context.start();
        fireEvent(StartedEvent.class, new StartedEvent());

        if (runtimeConfig.dumpRoutes) {
            dumpRoutes();
        }
    }

    @Override
    protected void doStop() throws Exception {
        fireEvent(StoppingEvent.class, new StoppingEvent());
        context.stop();
        fireEvent(StoppedEvent.class, new StoppedEvent());
        if (context instanceof ShutdownableService) {
            ((ShutdownableService) context).shutdown();
        }
    }

    protected void loadRoutes(CamelContext context) throws Exception {
        for (RoutesBuilder b : builders) {
            context.addRoutes(b);
        }

        String routesUri = buildTimeConfig.routesUri;
        if (ObjectHelper.isNotEmpty(routesUri)) {
            log.info("routesUri: {}", routesUri);
            log.info("cur dir: {}", new File(".").getCanonicalFile().toString());

            ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
            try (InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(mcc, routesUri)) {
                mcc.addRouteDefinitions(is);
            }
        }
    }

    protected String getProperty(String name) throws Exception {
        return context.resolvePropertyPlaceholders(context.getPropertyPrefixToken() + name + context.getPropertySuffixToken());
    }

    protected CamelContext createContext() {
        FastCamelContext context = new FastCamelContext();
        context.setRegistry(registry);
        return context;
    }

    protected <T> void fireEvent(Class<T> clazz, T event) {
        Arc.container().beanManager().getEvent().select(clazz).fire(event);
    }

    public void setBeanContainer(BeanContainer beanContainer) {
        this.beanContainer = beanContainer;
    }

    public void setRegistry(Registry registry) {
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

    @Override
    public Registry getRegistry() {
        return registry;
    }

    @Override
    public BuildTime getBuildTimeConfig() {
        return buildTimeConfig;
    }

    @Override
    public Runtime getRuntimeConfig() {
        return runtimeConfig;
    }

    protected PropertiesComponent createPropertiesComponent(Properties initialPoperties) {
        PropertiesComponent pc = new PropertiesComponent();
        pc.setInitialProperties(initialPoperties);

        RuntimeSupport.bindProperties(properties, pc, PFX_CAMEL_PROPERTIES);

        return pc;
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

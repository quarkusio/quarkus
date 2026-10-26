package io.quarkus.resteasy.reactive.server.servlet.deployment;

import jakarta.servlet.DispatcherType;
import jakarta.ws.rs.core.Application;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.server.deployment.RequestContextFactoryBuildItem;
import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveDeploymentBuildItem;
import io.quarkus.resteasy.reactive.server.servlet.runtime.ResteasyReactiveFilter;
import io.quarkus.resteasy.reactive.server.servlet.runtime.ResteasyReactiveServlet;
import io.quarkus.resteasy.reactive.server.servlet.runtime.ServletRequestContextFactory;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;

/**
 * Processor that finds JAX-RS classes in the deployment
 */
public class ResteasyReactiveServletProcessor {

    private static final String JAVAX_WS_RS_APPLICATION = Application.class.getName();
    private static final String JAX_RS_FILTER_NAME = JAVAX_WS_RS_APPLICATION;
    private static final String JAX_RS_SERVLET_NAME = JAVAX_WS_RS_APPLICATION;

    @BuildStep
    public RequestContextFactoryBuildItem contextFactoryBuildItem() {
        return new RequestContextFactoryBuildItem(ServletRequestContextFactory.INSTANCE);
    }

    @BuildStep
    public void build(ResteasyReactiveDeploymentBuildItem deploymentBuildItem,
            BuildProducer<FilterBuildItem> filter,
            BuildProducer<ServletBuildItem> servlet,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) throws Exception {
        if (deploymentBuildItem == null) {
            return;
        }
        String path = deploymentBuildItem.getApplicationPath();
        //if JAX-RS is installed at the root location we use a filter, otherwise we use a Servlet and take over the whole mapped path
        if (path.equals("/") || path.isEmpty()) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(ResteasyReactiveFilter.class).constructors().build());
            filter.produce(
                    FilterBuildItem.builder(JAX_RS_FILTER_NAME, ResteasyReactiveFilter.class.getName()).setLoadOnStartup(1)
                            .addFilterServletNameMapping("default", DispatcherType.REQUEST)
                            .addFilterServletNameMapping("default", DispatcherType.FORWARD)
                            .addFilterServletNameMapping("default", DispatcherType.INCLUDE)
                            .setAsyncSupported(true)
                            .build());
        } else {
            String mappingPath = deploymentBuildItem.getApplicationPath();
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(ResteasyReactiveServlet.class).constructors().build());
            servlet.produce(ServletBuildItem.builder(JAX_RS_SERVLET_NAME, ResteasyReactiveServlet.class.getName())
                    .setLoadOnStartup(1).addMapping(mappingPath + "/*").setAsyncSupported(true).build());
        }
    }
}

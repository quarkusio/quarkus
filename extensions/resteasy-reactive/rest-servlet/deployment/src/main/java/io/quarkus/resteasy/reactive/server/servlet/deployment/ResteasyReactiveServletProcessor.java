package io.quarkus.resteasy.reactive.server.servlet.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import jakarta.servlet.DispatcherType;
import jakarta.ws.rs.core.Application;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.resteasy.reactive.server.deployment.RequestContextFactoryBuildItem;
import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveDeploymentBuildItem;
import io.quarkus.resteasy.reactive.server.servlet.runtime.ResteasyReactiveFilter;
import io.quarkus.resteasy.reactive.server.servlet.runtime.ResteasyReactiveServlet;
import io.quarkus.resteasy.reactive.server.servlet.runtime.ResteasyReactiveServletRecorder;
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
    @Record(STATIC_INIT)
    public void build(ResteasyReactiveServletRecorder restRecorder,
            ResteasyReactiveDeploymentBuildItem deploymentBuildItem,
            BuildProducer<FilterBuildItem> filter,
            BuildProducer<ServletBuildItem> servlet) throws Exception {

        if (deploymentBuildItem == null) {
            return;
        }

        String path = deploymentBuildItem.getApplicationPath();

        //if JAX-RS is installed at the root location we use a filter, otherwise we use a Servlet and take over the whole mapped path
        if (path.equals("/") || path.isEmpty()) {
            filter.produce(
                    FilterBuildItem.builder(JAX_RS_FILTER_NAME, ResteasyReactiveFilter.class.getName()).setLoadOnStartup(1)
                            .addFilterServletNameMapping("default", DispatcherType.REQUEST)
                            .addFilterServletNameMapping("default", DispatcherType.FORWARD)
                            .addFilterServletNameMapping("default", DispatcherType.INCLUDE)
                            .setInstanceFactory(restRecorder.filter(deploymentBuildItem.getDeployment()))
                            .setAsyncSupported(true)
                            .build());
        } else {
            String mappingPath = deploymentBuildItem.getApplicationPath();
            servlet.produce(ServletBuildItem.builder(JAX_RS_SERVLET_NAME, ResteasyReactiveServlet.class.getName())
                    .setInstanceFactory(restRecorder.servlet(deploymentBuildItem.getDeployment()))
                    .setLoadOnStartup(1).addMapping(mappingPath + "/*").setAsyncSupported(true).build());
        }

    }

}

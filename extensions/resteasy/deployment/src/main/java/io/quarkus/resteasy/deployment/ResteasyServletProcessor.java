package io.quarkus.resteasy.deployment;

import java.util.Map.Entry;
import java.util.Optional;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.Application;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.resteasy.runtime.ResteasyFilter;
import io.quarkus.resteasy.server.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyServerConfigBuildItem;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;

/**
 * Processor that finds JAX-RS classes in the deployment
 */
public class ResteasyServletProcessor {
    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    private static final String JAVAX_WS_RS_APPLICATION = Application.class.getName();
    private static final String JAX_RS_FILTER_NAME = JAVAX_WS_RS_APPLICATION;
    private static final String JAX_RS_SERVLET_NAME = JAVAX_WS_RS_APPLICATION;

    @BuildStep
    public void jaxrsConfig(Optional<ResteasyServerConfigBuildItem> resteasyServerConfig,
            BuildProducer<ResteasyJaxrsConfigBuildItem> resteasyJaxrsConfig) {
        if (resteasyServerConfig.isPresent()) {
            resteasyJaxrsConfig.produce(new ResteasyJaxrsConfigBuildItem(resteasyServerConfig.get().getPath()));
        }
    }

    @BuildStep
    public void build(
            Capabilities capabilities,
            Optional<ResteasyServerConfigBuildItem> resteasyServerConfig,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<FilterBuildItem> filter,
            BuildProducer<ServletBuildItem> servlet,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServletInitParamBuildItem> servletInitParameters,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady) throws Exception {
        if (!capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
            log.info(
                    "Resteasy running without servlet container.  Add quarkus-undertow if you want Restasy to run within a servlet container");
            return;
        }
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY));

        if (resteasyServerConfig.isPresent()) {
            String path = resteasyServerConfig.get().getPath();

            //if JAX-RS is installed at the root location we use a filter, otherwise we use a Servlet and take over the whole mapped path
            if (path.equals("/") || path.isEmpty()) {
                filter.produce(FilterBuildItem.builder(JAX_RS_FILTER_NAME, ResteasyFilter.class.getName()).setLoadOnStartup(1)
                        .addFilterServletNameMapping("default", DispatcherType.REQUEST).setAsyncSupported(true)
                        .build());
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ResteasyFilter.class.getName()));
            } else {
                String mappingPath = getMappingPath(path);
                servlet.produce(ServletBuildItem.builder(JAX_RS_SERVLET_NAME, HttpServlet30Dispatcher.class.getName())
                        .setLoadOnStartup(1).addMapping(mappingPath).setAsyncSupported(true).build());
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, HttpServlet30Dispatcher.class.getName()));
            }

            for (Entry<String, String> initParameter : resteasyServerConfig.get().getInitParameters().entrySet()) {
                servletInitParameters.produce(new ServletInitParamBuildItem(initParameter.getKey(), initParameter.getValue()));
            }
        }
    }

    private String getMappingPath(String path) {
        String mappingPath;
        if (path.endsWith("/")) {
            mappingPath = path + "*";
        } else {
            mappingPath = path + "/*";
        }
        return mappingPath;
    }
}

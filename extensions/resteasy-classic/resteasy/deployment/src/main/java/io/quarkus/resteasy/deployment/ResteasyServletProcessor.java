package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.Application;

import org.jboss.logging.Logger;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.runtime.ExceptionMapperRecorder;
import io.quarkus.resteasy.runtime.ResteasyFilter;
import io.quarkus.resteasy.runtime.ResteasyServlet;
import io.quarkus.resteasy.server.common.deployment.ResteasyServerConfigBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyServletMappingBuildItem;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletContextPathBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;
import io.quarkus.undertow.deployment.WebMetadataBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;

/**
 * Processor that finds JAX-RS classes in the deployment
 */
public class ResteasyServletProcessor {
    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    private static final String JAVAX_WS_RS_APPLICATION = Application.class.getName();
    private static final String JAX_RS_FILTER_NAME = JAVAX_WS_RS_APPLICATION;
    private static final String JAX_RS_SERVLET_NAME = JAVAX_WS_RS_APPLICATION;

    @BuildStep
    public void jaxrsConfig(
            Optional<ResteasyServerConfigBuildItem> resteasyServerConfig,
            BuildProducer<ResteasyJaxrsConfigBuildItem> deprecatedResteasyJaxrsConfig,
            BuildProducer<io.quarkus.resteasy.server.common.spi.ResteasyJaxrsConfigBuildItem> resteasyJaxrsConfig,
            HttpRootPathBuildItem httpRootPathBuildItem) {
        if (resteasyServerConfig.isPresent()) {
            String rp = resteasyServerConfig.get().getRootPath();
            String rootPath = httpRootPathBuildItem.resolvePath(rp.startsWith("/") ? rp.substring(1) : rp);
            String defaultPath = httpRootPathBuildItem.resolvePath(resteasyServerConfig.get().getPath());

            deprecatedResteasyJaxrsConfig.produce(new ResteasyJaxrsConfigBuildItem(defaultPath));
            resteasyJaxrsConfig
                    .produce(new io.quarkus.resteasy.server.common.spi.ResteasyJaxrsConfigBuildItem(rootPath, defaultPath));
        }
    }

    @BuildStep
    public ResteasyServletMappingBuildItem webXmlMapping(Optional<WebMetadataBuildItem> webMetadataBuildItem) {
        if (webMetadataBuildItem.isPresent()) {
            List<ServletMappingMetaData> servletMappings = webMetadataBuildItem.get().getWebMetaData().getServletMappings();
            if (servletMappings != null) {
                for (ServletMappingMetaData mapping : servletMappings) {
                    if (JAVAX_WS_RS_APPLICATION.equals(mapping.getServletName())) {
                        if (!mapping.getUrlPatterns().isEmpty()) {
                            return new ResteasyServletMappingBuildItem(mapping.getUrlPatterns().iterator().next());
                        }
                    }
                }
            }
        }
        return null;
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
            Optional<ServletContextPathBuildItem> servletContextPathBuildItem,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady) {

        if (!capabilities.isPresent(Capability.SERVLET)) {
            return;
        }
        feature.produce(new FeatureBuildItem(Feature.RESTEASY));

        if (resteasyServerConfig.isPresent()) {
            String path = resteasyServerConfig.get().getPath();

            //if JAX-RS is installed at the root location we use a filter, otherwise we use a Servlet and take over the whole mapped path
            if (path.equals("/") || path.isEmpty()) {
                filter.produce(FilterBuildItem.builder(JAX_RS_FILTER_NAME, ResteasyFilter.class.getName()).setLoadOnStartup(1)
                        .addFilterServletNameMapping("default", DispatcherType.REQUEST)
                        .addFilterServletNameMapping("default", DispatcherType.FORWARD)
                        .addFilterServletNameMapping("default", DispatcherType.INCLUDE).setAsyncSupported(true)
                        .build());
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ResteasyFilter.class.getName()));
            } else {
                String mappingPath = getMappingPath(path);
                servlet.produce(ServletBuildItem.builder(JAX_RS_SERVLET_NAME, ResteasyServlet.class.getName())
                        .setLoadOnStartup(1).addMapping(mappingPath).setAsyncSupported(true).build());
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, HttpServlet30Dispatcher.class.getName()));
            }

            for (Entry<String, String> initParameter : resteasyServerConfig.get().getInitParameters().entrySet()) {
                servletInitParameters
                        .produce(new ServletInitParamBuildItem(initParameter.getKey(), initParameter.getValue()));
            }
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(STATIC_INIT)
    void addServletsToExceptionMapper(List<ServletBuildItem> servlets, ExceptionMapperRecorder recorder) {
        recorder.setServlets(servlets.stream().filter(s -> !JAX_RS_SERVLET_NAME.equals(s.getName()))
                .collect(Collectors.toMap(s -> s.getName(), s -> s.getMappings())));
    }

    private String getMappingPath(String path) {
        String mappingPath;
        if (path.endsWith("/*")) {
            return path;
        }
        if (path.endsWith("/")) {
            mappingPath = path + "*";
        } else {
            mappingPath = path + "/*";
        }
        return mappingPath;
    }
}

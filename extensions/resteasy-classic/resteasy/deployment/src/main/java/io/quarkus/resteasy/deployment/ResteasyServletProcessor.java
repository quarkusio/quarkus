package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.ws.rs.core.Application;

import org.jboss.logging.Logger;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.runtime.ResteasyFilter;
import io.quarkus.resteasy.runtime.ResteasyServlet;
import io.quarkus.resteasy.runtime.ResteasyServletRecorder;
import io.quarkus.resteasy.runtime.ServletStaticResourceShadowingCheck;
import io.quarkus.resteasy.server.common.deployment.ResteasyServerConfigBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyServletMappingBuildItem;
import io.quarkus.resteasy.server.common.spi.ResteasyJaxrsConfigBuildItem;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.deployment.KnownPathsBuildItem;
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
            BuildProducer<ResteasyJaxrsConfigBuildItem> resteasyJaxrsConfig,
            HttpRootPathBuildItem httpRootPathBuildItem) {
        if (resteasyServerConfig.isPresent()) {
            String rootPath = httpRootPathBuildItem.relativePath(resteasyServerConfig.get().getRootPath());
            String defaultPath = resteasyServerConfig.get().getPath();

            resteasyJaxrsConfig.produce(new ResteasyJaxrsConfigBuildItem(rootPath, defaultPath));
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
            if (isRootPath(path)) {
                filter.produce(FilterBuildItem.builder(JAX_RS_FILTER_NAME, ResteasyFilter.class.getName()).setLoadOnStartup(1)
                        .addFilterServletNameMapping("default", DispatcherType.REQUEST)
                        .addFilterServletNameMapping("default", DispatcherType.FORWARD)
                        .addFilterServletNameMapping("default", DispatcherType.INCLUDE).setAsyncSupported(true)
                        .build());
                reflectiveClass.produce(
                        ReflectiveClassBuildItem.builder(ResteasyFilter.class.getName()).build());
            } else {
                String mappingPath = getMappingPath(path);
                servlet.produce(ServletBuildItem.builder(JAX_RS_SERVLET_NAME, ResteasyServlet.class.getName())
                        .setLoadOnStartup(1).addMapping(mappingPath).setAsyncSupported(true).build());
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(HttpServlet30Dispatcher.class.getName())
                        .build());
            }

            for (Entry<String, String> initParameter : resteasyServerConfig.get().getInitParameters().entrySet()) {
                servletInitParameters
                        .produce(new ServletInitParamBuildItem(initParameter.getKey(), initParameter.getValue()));
            }
        }
    }

    @BuildStep
    AdditionalBeanBuildItem servletStaticResourceShadowingCheck(Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.SERVLET)) {
            return null;
        }
        // runs the check at startup when the static resource paths are set, see checkStaticResourceShadowing
        return AdditionalBeanBuildItem.builder().addBeanClass(ServletStaticResourceShadowingCheck.class)
                .setDefaultScope(DotNames.SINGLETON).build();
    }

    // produces ServiceStartBuildItem to set the static resource paths before the StartupEvent, on which the check runs
    @BuildStep
    @Record(RUNTIME_INIT)
    @Produce(ServiceStartBuildItem.class)
    void checkStaticResourceShadowing(ResteasyServletRecorder recorder,
            Capabilities capabilities,
            Optional<ResteasyServerConfigBuildItem> resteasyServerConfig,
            Optional<KnownPathsBuildItem> knownPaths,
            Optional<WebMetadataBuildItem> webMetadata) {
        // only the filter mapped to the default servlet lets it serve static resources before RESTEasy, the servlet
        // takes over its whole path
        if (!capabilities.isPresent(Capability.SERVLET) || resteasyServerConfig.isEmpty()
                || !isRootPath(resteasyServerConfig.get().getPath()) || knownPaths.isEmpty()) {
            return;
        }
        Set<String> staticFiles = knownPaths.get().knownFiles;
        // the known paths are empty in dev mode, the static resources are then served from the source directories
        if (!staticFiles.isEmpty()) {
            // records the file paths rather than the request paths: the servlet container records the same strings,
            // see UndertowBuildStep#build, and equal string constants are shared, so the application keeps no copy
            recorder.setStaticResources(staticFiles, welcomeFiles(webMetadata));
        }
    }

    private static List<String> welcomeFiles(Optional<WebMetadataBuildItem> webMetadata) {
        List<String> welcomeFiles = null;
        if (webMetadata.isPresent() && webMetadata.get().getWebMetaData().getWelcomeFileList() != null) {
            welcomeFiles = webMetadata.get().getWebMetaData().getWelcomeFileList().getWelcomeFiles();
        }
        return welcomeFiles != null ? welcomeFiles : List.of("index.html", "index.htm");
    }

    private static boolean isRootPath(String path) {
        return path.equals("/") || path.isEmpty();
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

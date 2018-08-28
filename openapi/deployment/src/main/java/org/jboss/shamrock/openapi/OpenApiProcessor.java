package org.jboss.shamrock.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.openapi.runtime.OpenApiDeploymentTemplate;
import org.jboss.shamrock.openapi.runtime.OpenApiDocumentProducer;
import org.jboss.shamrock.openapi.runtime.OpenApiServlet;
import org.jboss.shamrock.undertow.ServletData;
import org.jboss.shamrock.undertow.ServletDeployment;
import org.jboss.shamrock.weld.deployment.WeldDeployment;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;

/**
 * @author Ken Finnigan
 */
public class OpenApiProcessor implements ResourceProcessor {

    @Inject
    private WeldDeployment weldDeployment;

    @Inject
    private ShamrockConfig config;

    @Inject
    private ServletDeployment servletDeployment;

    private OpenApiSerializer.Format format = OpenApiSerializer.Format.YAML;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        ServletData servletData = new ServletData("openapi", OpenApiServlet.class.getName());
        servletData.getMapings().add(config.getConfig("openapi.path", "/openapi"));
        servletDeployment.addServlet(servletData);
        weldDeployment.addAdditionalBean(OpenApiServlet.class);
        weldDeployment.addAdditionalBean(OpenApiDocumentProducer.class);

        String resourcePath = findStaticModel(archiveContext);

        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.WELD_DEPLOYMENT + 30)) {
            OpenApiDeploymentTemplate template = recorder.getRecordingProxy(OpenApiDeploymentTemplate.class);
            OpenAPI sm = generateStaticModel(resourcePath, format);
            OpenAPI am = generateAnnotationModel(archiveContext.getCombinedIndex());
            template.setupModel(null, sm, am);
        }
    }


    public OpenAPI generateStaticModel(String resourcePath, OpenApiSerializer.Format format) {
        if (resourcePath != null) {
            try (InputStream is = new URL(resourcePath).openStream()) {
                try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, format)) {
                    return io.smallrye.openapi.runtime.OpenApiProcessor.modelFromStaticFile(staticFile);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Ignore
        }

        return null;
    }

    public OpenAPI generateAnnotationModel(IndexView indexView) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);
        return new OpenApiAnnotationScanner(openApiConfig, indexView).scan();
    }

    @Override
    public int getPriority() {
        return 1;
    }

    private String findStaticModel(ArchiveContext archiveContext) {
        // Check for the file in both META-INF and WEB-INF/classes/META-INF
        Path resourcePath = archiveContext.getRootArchive().getChildPath("META-INF/openapi.yaml");
        if (resourcePath == null) {
            resourcePath = archiveContext.getRootArchive().getChildPath("WEB-INF/classes/META-INF/openapi.yaml");
        }
        if (resourcePath == null) {
            resourcePath = archiveContext.getRootArchive().getChildPath("META-INF/openapi.yml");
        }
        if (resourcePath == null) {
            resourcePath = archiveContext.getRootArchive().getChildPath("WEB-INF/classes/META-INF/openapi.yml");
        }
        if (resourcePath == null) {
            resourcePath = archiveContext.getRootArchive().getChildPath("META-INF/openapi.json");
            format = OpenApiSerializer.Format.JSON;
        }
        if (resourcePath == null) {
            resourcePath = archiveContext.getRootArchive().getChildPath("WEB-INF/classes/META-INF/openapi.json");
            format = OpenApiSerializer.Format.JSON;
        }

        if (resourcePath == null) {
            return null;
        }

        return archiveContext.getRootArchive().getArchiveRoot().relativize(resourcePath).toString();
    }
}

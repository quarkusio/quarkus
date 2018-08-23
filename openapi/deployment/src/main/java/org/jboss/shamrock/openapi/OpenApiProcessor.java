package org.jboss.shamrock.openapi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.IndexWriter;
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
        String indexPath = writeIndex(archiveContext.getRootArchive().getIndex());

        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.WELD_DEPLOYMENT + 30)) {
            OpenApiDeploymentTemplate template = recorder.getRecordingProxy(OpenApiDeploymentTemplate.class);

            template.generateStaticModel(resourcePath, format);
            template.generateAnnotationModel(indexPath);
        }

        try (BytecodeRecorder recorder = processorContext.addDeploymentTask(RuntimePriority.WELD_DEPLOYMENT + 30)) {
            OpenApiDeploymentTemplate template = recorder.getRecordingProxy(OpenApiDeploymentTemplate.class);
            template.setupModel(null, null, null);
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }

    private String writeIndex(IndexView index) {
        try {
            Path path = Files.createTempFile("shamrock-index", ".jandex");
            OutputStream os = new FileOutputStream(path.toString());
            IndexWriter writer = new IndexWriter(os);
            writer.write((Index) index);
            return path.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
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

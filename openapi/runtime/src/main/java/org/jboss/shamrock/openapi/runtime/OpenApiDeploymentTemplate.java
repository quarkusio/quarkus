package org.jboss.shamrock.openapi.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.enterprise.inject.se.SeContainer;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.Shamrock;

/**
 * @author Ken Finnigan
 */
public class OpenApiDeploymentTemplate {

    @ContextObject("openapiStaticModel")
    public OpenAPI generateStaticModel(String resourcePath, OpenApiSerializer.Format format) {
        if (resourcePath != null) {
            try (InputStream is = new URL(resourcePath).openStream()) {
                try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, format)) {
                    return OpenApiProcessor.modelFromStaticFile(staticFile);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Ignore
        }

        return null;
    }

    @ContextObject("openapiAnnotationModel")
    public OpenAPI generateAnnotationModel(String indexPath) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

        Index index = null;

        try {
            FileInputStream fis = new FileInputStream(new File(indexPath));
            IndexReader reader = new IndexReader(fis);
            index = reader.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new OpenApiAnnotationScanner(openApiConfig, index).scan();
    }

    public void setupModel(@ContextObject("weld.container") SeContainer container,
                           @ContextObject("openapiStaticModel") OpenAPI staticModel,
                           @ContextObject("openapiAnnotationModel") OpenAPI annotationModel) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

        OpenAPI readerModel = OpenApiProcessor.modelFromReader(openApiConfig, Shamrock.class.getClassLoader());

        OpenApiDocument document = createDocument(openApiConfig);
        document.modelFromAnnotations(annotationModel);
        document.modelFromReader(readerModel);
        document.modelFromStaticFile(staticModel);
        document.filter(filter(openApiConfig));
        document.initialize();

        container.select(OpenApiDocumentProducer.class).get().setDocument(document);
    }

    private OpenApiDocument createDocument(OpenApiConfig openApiConfig) {
        OpenApiDocument document = OpenApiDocument.INSTANCE;
        document.reset();
        document.config(openApiConfig);
        return document;
    }

    private OASFilter filter(OpenApiConfig openApiConfig) {
        return OpenApiProcessor.getFilter(openApiConfig, Shamrock.class.getClassLoader());
    }
}

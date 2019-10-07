package io.quarkus.smallrye.openapi.runtime;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

/**
 * @author Ken Finnigan
 */
@ApplicationScoped
public class OpenApiDocumentProducer {
    private OpenApiDocument document;

    /**
     * We load the document from the generated JSON file, which should have all the annotations
     *
     * Most apps will likely just want to serve the OpenAPI doc, rather than inject it, which is why we generated the
     * static file and parse it if required. The more Quarkus-like approach of serializing the doc to bytecode
     * can result in a lot of bytecode, which will likely just be turned straight into a static file anyway.
     */
    @PostConstruct
    void create() throws IOException {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(OpenApiHandler.BASE_NAME + OpenApiSerializer.Format.JSON)) {
            if (is != null) {
                try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, OpenApiSerializer.Format.JSON)) {
                    Config config = ConfigProvider.getConfig();
                    OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

                    OpenAPI readerModel = OpenApiProcessor.modelFromReader(openApiConfig,
                            Thread.currentThread().getContextClassLoader());
                    document = OpenApiDocument.INSTANCE;
                    document.reset();
                    document.config(openApiConfig);

                    document.modelFromReader(readerModel);
                    document.modelFromStaticFile(io.smallrye.openapi.runtime.OpenApiProcessor.modelFromStaticFile(staticFile));
                    document.filter(OpenApiProcessor.getFilter(openApiConfig, Thread.currentThread().getContextClassLoader()));
                    document.initialize();
                }
            }
        }

    }

    @Produces
    @Dependent
    OpenApiDocument openApiDocument() {
        return this.document;
    }

}

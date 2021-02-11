package io.quarkus.smallrye.openapi.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

/**
 * Loads the document and make it available
 */
@ApplicationScoped
public class OpenApiDocumentService {

    private byte[] jsonDocument;
    private byte[] yamlDocument;

    @PostConstruct
    void create() throws IOException {

        ClassLoader cl = OpenApiConstants.classLoader == null ? Thread.currentThread().getContextClassLoader()
                : OpenApiConstants.classLoader;
        try (InputStream is = cl.getResourceAsStream(OpenApiConstants.BASE_NAME + Format.JSON)) {
            if (is != null) {
                try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {
                    Config config = ConfigProvider.getConfig();
                    OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

                    OpenApiDocument document = OpenApiDocument.INSTANCE;
                    document.reset();
                    document.config(openApiConfig);
                    document.modelFromStaticFile(OpenApiProcessor.modelFromStaticFile(staticFile));
                    document.filter(OpenApiProcessor.getFilter(openApiConfig, cl));
                    document.initialize();

                    this.jsonDocument = OpenApiSerializer.serialize(document.get(), Format.JSON)
                            .getBytes(StandardCharsets.UTF_8);
                    this.yamlDocument = OpenApiSerializer.serialize(document.get(), Format.YAML)
                            .getBytes(StandardCharsets.UTF_8);
                    document.reset();
                    document = null;
                }
            } else {
                throw new IOException("Could not find [" + OpenApiConstants.BASE_NAME + Format.JSON + "]");
            }
        }
    }

    public byte[] getJsonDocument() {
        return jsonDocument;
    }

    public byte[] getYamlDocument() {
        return yamlDocument;
    }

    public byte[] getDocument(Format format) {
        if (format.equals(Format.JSON)) {
            return getJsonDocument();
        }
        return getYamlDocument();
    }
}

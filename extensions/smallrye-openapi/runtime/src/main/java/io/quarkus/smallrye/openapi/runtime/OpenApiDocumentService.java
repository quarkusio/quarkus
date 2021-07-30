package io.quarkus.smallrye.openapi.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

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
public class OpenApiDocumentService implements OpenApiDocumentHolder {

    private boolean alwaysRunFilter;

    private OpenApiDocumentHolder documentHolder;

    @PostConstruct
    void create() throws IOException {

        Config config = ConfigProvider.getConfig();
        this.alwaysRunFilter = config.getOptionalValue("quarkus.smallrye-openapi.always-run-filter", Boolean.class)
                .orElse(Boolean.FALSE);
        if (alwaysRunFilter) {
            this.documentHolder = new DynamicDocument(config);
        } else {
            this.documentHolder = new StaticDocument(config);
        }
    }

    public byte[] getJsonDocument() {
        return this.documentHolder.getJsonDocument();
    }

    public byte[] getYamlDocument() {
        return this.documentHolder.getYamlDocument();
    }

    /**
     * Generate the document once on creation.
     */
    class StaticDocument implements OpenApiDocumentHolder {

        private byte[] jsonDocument;
        private byte[] yamlDocument;

        StaticDocument(Config config) {
            ClassLoader cl = OpenApiConstants.classLoader == null ? Thread.currentThread().getContextClassLoader()
                    : OpenApiConstants.classLoader;
            try (InputStream is = cl.getResourceAsStream(OpenApiConstants.BASE_NAME + Format.JSON)) {
                if (is != null) {
                    try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {

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
                }
            } catch (IOException ex) {
                throw new RuntimeException("Could not find [" + OpenApiConstants.BASE_NAME + Format.JSON + "]");
            }
        }

        public byte[] getJsonDocument() {
            return this.jsonDocument;
        }

        public byte[] getYamlDocument() {
            return this.yamlDocument;
        }
    }

    /**
     * Generate the document on every request.
     */
    class DynamicDocument implements OpenApiDocumentHolder {

        private OpenAPI generatedOnBuild;
        private OpenApiConfig openApiConfig;
        private OASFilter filter;

        DynamicDocument(Config config) {
            ClassLoader cl = OpenApiConstants.classLoader == null ? Thread.currentThread().getContextClassLoader()
                    : OpenApiConstants.classLoader;
            try (InputStream is = cl.getResourceAsStream(OpenApiConstants.BASE_NAME + Format.JSON)) {
                if (is != null) {
                    try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {
                        this.generatedOnBuild = OpenApiProcessor.modelFromStaticFile(staticFile);
                        this.openApiConfig = new OpenApiConfigImpl(config);
                        this.filter = OpenApiProcessor.getFilter(openApiConfig, cl);
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException("Could not find [" + OpenApiConstants.BASE_NAME + Format.JSON + "]");
            }
        }

        public byte[] getJsonDocument() {
            try {
                OpenApiDocument document = getOpenApiDocument();
                byte[] jsonDocument = OpenApiSerializer.serialize(document.get(), Format.JSON)
                        .getBytes(StandardCharsets.UTF_8);
                document.reset();
                document = null;
                return jsonDocument;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        public byte[] getYamlDocument() {
            try {
                OpenApiDocument document = getOpenApiDocument();
                byte[] yamlDocument = OpenApiSerializer.serialize(document.get(), Format.YAML)
                        .getBytes(StandardCharsets.UTF_8);
                document.reset();
                document = null;
                return yamlDocument;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private OpenApiDocument getOpenApiDocument() {
            OpenApiDocument document = OpenApiDocument.INSTANCE;
            document.reset();
            document.config(this.openApiConfig);
            document.modelFromStaticFile(this.generatedOnBuild);
            document.filter(this.filter);
            document.initialize();
            return document;
        }
    }
}

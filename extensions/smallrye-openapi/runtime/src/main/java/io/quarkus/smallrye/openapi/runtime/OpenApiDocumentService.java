package io.quarkus.smallrye.openapi.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.smallrye.openapi.runtime.filter.DisabledRestEndpointsFilter;
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

    private static final String OPENAPI_SERVERS = "mp.openapi.servers";
    private static final IndexView EMPTY_INDEX = new Indexer().complete();
    private final OpenApiDocumentHolder documentHolder;
    private final String previousOpenApiServersSystemPropertyValue;

    public OpenApiDocumentService(OASFilter autoSecurityFilter, Config config) {

        String servers = config.getOptionalValue("quarkus.smallrye-openapi.servers", String.class).orElse(null);
        this.previousOpenApiServersSystemPropertyValue = System.getProperty(OPENAPI_SERVERS);
        if (servers != null && !servers.isEmpty()) {
            System.setProperty(OPENAPI_SERVERS, servers);
        }

        if (config.getOptionalValue("quarkus.smallrye-openapi.always-run-filter", Boolean.class).orElse(Boolean.FALSE)) {
            this.documentHolder = new DynamicDocument(config, autoSecurityFilter);
        } else {
            this.documentHolder = new StaticDocument(config, autoSecurityFilter);
        }
    }

    void reset(@Observes ShutdownEvent event) {
        // Reset the value of the System property "mp.openapi.servers" to prevent side effects on tests since
        // the value of System property "mp.openapi.servers" takes precedence over the value of
        // "quarkus.smallrye-openapi.servers" due to the configuration mapping
        if (previousOpenApiServersSystemPropertyValue == null) {
            System.clearProperty(OPENAPI_SERVERS);
        } else {
            System.setProperty(OPENAPI_SERVERS, previousOpenApiServersSystemPropertyValue);
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
    static class StaticDocument implements OpenApiDocumentHolder {

        private byte[] jsonDocument;
        private byte[] yamlDocument;

        StaticDocument(Config config, OASFilter autoFilter) {
            ClassLoader cl = OpenApiConstants.classLoader == null ? Thread.currentThread().getContextClassLoader()
                    : OpenApiConstants.classLoader;
            try (InputStream is = cl.getResourceAsStream(OpenApiConstants.BASE_NAME + Format.JSON)) {
                if (is != null) {
                    try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {

                        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

                        OpenApiDocument document = OpenApiDocument.INSTANCE;
                        document.reset();
                        document.config(openApiConfig);
                        document.modelFromStaticFile(OpenApiProcessor.modelFromStaticFile(openApiConfig, staticFile));
                        if (autoFilter != null) {
                            document.filter(autoFilter);
                        }
                        document.filter(new DisabledRestEndpointsFilter());
                        document.filter(OpenApiProcessor.getFilter(openApiConfig, cl, EMPTY_INDEX));
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
    static class DynamicDocument implements OpenApiDocumentHolder {

        private OpenAPI generatedOnBuild;
        private OpenApiConfig openApiConfig;
        private OASFilter userFilter;
        private OASFilter autoFilter;
        private DisabledRestEndpointsFilter disabledEndpointsFilter;

        DynamicDocument(Config config, OASFilter autoFilter) {
            ClassLoader cl = OpenApiConstants.classLoader == null ? Thread.currentThread().getContextClassLoader()
                    : OpenApiConstants.classLoader;
            try (InputStream is = cl.getResourceAsStream(OpenApiConstants.BASE_NAME + Format.JSON)) {
                if (is != null) {
                    try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {
                        this.openApiConfig = new OpenApiConfigImpl(config);
                        this.userFilter = OpenApiProcessor.getFilter(openApiConfig, cl, EMPTY_INDEX);
                        this.autoFilter = autoFilter;
                        this.generatedOnBuild = OpenApiProcessor.modelFromStaticFile(this.openApiConfig, staticFile);
                        this.disabledEndpointsFilter = new DisabledRestEndpointsFilter();
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
            if (this.autoFilter != null) {
                document.filter(this.autoFilter);
            }
            document.filter(this.disabledEndpointsFilter);
            document.filter(this.userFilter);
            document.initialize();
            return document;
        }
    }
}

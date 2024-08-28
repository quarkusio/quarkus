package io.quarkus.smallrye.openapi.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

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

    private static final IndexView EMPTY_INDEX = new Indexer().complete();
    private final OpenApiDocumentHolder documentHolder;

    @Inject
    public OpenApiDocumentService(OASFilter autoSecurityFilter,
            OpenApiRecorder.UserDefinedRuntimeFilters userDefinedRuntimeFilters, Config config) {

        if (config.getOptionalValue("quarkus.smallrye-openapi.always-run-filter", Boolean.class).orElse(Boolean.FALSE)) {
            this.documentHolder = new DynamicDocument(config, autoSecurityFilter, userDefinedRuntimeFilters.filters());
        } else {
            this.documentHolder = new StaticDocument(config, autoSecurityFilter, userDefinedRuntimeFilters.filters());
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

        StaticDocument(Config config, OASFilter autoFilter, List<String> userFilters) {
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
                        for (String userFilter : userFilters) {
                            document.filter(OpenApiProcessor.getFilter(userFilter, cl, EMPTY_INDEX));
                        }
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
        private List<OASFilter> userFilters = new ArrayList<>();
        private OASFilter autoFilter;
        private DisabledRestEndpointsFilter disabledEndpointsFilter;

        DynamicDocument(Config config, OASFilter autoFilter, List<String> annotatedUserFilters) {
            ClassLoader cl = OpenApiConstants.classLoader == null ? Thread.currentThread().getContextClassLoader()
                    : OpenApiConstants.classLoader;
            try (InputStream is = cl.getResourceAsStream(OpenApiConstants.BASE_NAME + Format.JSON)) {
                if (is != null) {
                    try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {
                        this.openApiConfig = new OpenApiConfigImpl(config);
                        OASFilter microProfileDefinedFilter = OpenApiProcessor.getFilter(openApiConfig, cl, EMPTY_INDEX);
                        if (microProfileDefinedFilter != null) {
                            userFilters.add(microProfileDefinedFilter);
                        }
                        for (String annotatedUserFilter : annotatedUserFilters) {
                            OASFilter annotatedUserDefinedFilter = OpenApiProcessor.getFilter(annotatedUserFilter, cl,
                                    EMPTY_INDEX);
                            userFilters.add(annotatedUserDefinedFilter);
                        }
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
            for (OASFilter userFilter : userFilters) {
                document.filter(userFilter);
            }
            document.initialize();
            return document;
        }
    }
}

package io.quarkus.smallrye.openapi.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.quarkus.smallrye.openapi.runtime.filter.DisabledRestEndpointsFilter;
import io.smallrye.openapi.api.SmallRyeOpenAPI;

/**
 * Loads the document and make it available
 */
@ApplicationScoped
public class OpenApiDocumentService implements OpenApiDocumentHolder {

    private final OpenApiDocumentHolder documentHolder;

    @Inject
    public OpenApiDocumentService(OASFilter autoSecurityFilter,
            OpenApiRecorder.UserDefinedRuntimeFilters userDefinedRuntimeFilters, Config config) {

        ClassLoader loader = Optional.ofNullable(OpenApiConstants.classLoader)
                .orElseGet(Thread.currentThread()::getContextClassLoader);

        try (InputStream source = loader.getResourceAsStream(OpenApiConstants.BASE_NAME + "JSON")) {
            if (source != null) {
                var userFilters = userDefinedRuntimeFilters.filters();
                boolean dynamic = config.getOptionalValue("quarkus.smallrye-openapi.always-run-filter", Boolean.class)
                        .orElse(Boolean.FALSE);

                if (dynamic) {
                    this.documentHolder = new DynamicDocument(source, config, autoSecurityFilter, userFilters);
                } else {
                    this.documentHolder = new StaticDocument(source, config, autoSecurityFilter, userFilters);
                }
            } else {
                this.documentHolder = new EmptyDocument();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] getJsonDocument() {
        return this.documentHolder.getJsonDocument();
    }

    public byte[] getYamlDocument() {
        return this.documentHolder.getYamlDocument();
    }

    static class EmptyDocument implements OpenApiDocumentHolder {
        static final byte[] EMPTY = new byte[0];

        public byte[] getJsonDocument() {
            return EMPTY;
        }

        public byte[] getYamlDocument() {
            return EMPTY;
        }
    }

    /**
     * Generate the document once on creation.
     */
    static class StaticDocument implements OpenApiDocumentHolder {

        private byte[] jsonDocument;
        private byte[] yamlDocument;

        StaticDocument(InputStream source, Config config, OASFilter autoFilter, List<String> userFilters) {
            SmallRyeOpenAPI.Builder builder = SmallRyeOpenAPI.builder()
                    .withConfig(config)
                    .enableModelReader(false)
                    .enableStandardStaticFiles(false)
                    .enableAnnotationScan(false)
                    .enableStandardFilter(false)
                    .withCustomStaticFile(() -> source);

            Optional.ofNullable(autoFilter).ifPresent(builder::addFilter);
            builder.addFilter(new DisabledRestEndpointsFilter());
            userFilters.forEach(builder::addFilterName);

            SmallRyeOpenAPI openAPI = builder.build();
            jsonDocument = openAPI.toJSON().getBytes(StandardCharsets.UTF_8);
            yamlDocument = openAPI.toYAML().getBytes(StandardCharsets.UTF_8);
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

        private SmallRyeOpenAPI.Builder builder;
        private OpenAPI generatedOnBuild;

        DynamicDocument(InputStream source, Config config, OASFilter autoFilter, List<String> annotatedUserFilters) {
            builder = SmallRyeOpenAPI.builder()
                    .withConfig(config)
                    .enableModelReader(false)
                    .enableStandardStaticFiles(false)
                    .enableAnnotationScan(false)
                    .enableStandardFilter(false)
                    .withCustomStaticFile(() -> source);

            generatedOnBuild = builder.build().model();

            builder.withCustomStaticFile(() -> null);
            builder.withInitialModel(generatedOnBuild);

            Optional.ofNullable(autoFilter).ifPresent(builder::addFilter);
            builder.addFilter(new DisabledRestEndpointsFilter());
            config.getOptionalValue(OASConfig.FILTER, String.class).ifPresent(builder::addFilterName);
            annotatedUserFilters.forEach(builder::addFilterName);
        }

        public byte[] getJsonDocument() {
            return builder.build().toJSON().getBytes(StandardCharsets.UTF_8);
        }

        public byte[] getYamlDocument() {
            return builder.build().toYAML().getBytes(StandardCharsets.UTF_8);
        }
    }
}

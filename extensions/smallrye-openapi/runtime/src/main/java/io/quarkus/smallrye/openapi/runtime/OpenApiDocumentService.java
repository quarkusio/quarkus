package io.quarkus.smallrye.openapi.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.IndexView;

import io.quarkus.smallrye.openapi.runtime.filter.DisabledRestEndpointsFilter;
import io.smallrye.openapi.api.SmallRyeOpenAPI;
import io.smallrye.openapi.runtime.io.Format;

/**
 * Loads the document and make it available
 */
@ApplicationScoped
public class OpenApiDocumentService {

    private final OpenApiDocumentHolder documentHolder;

    @Inject
    public OpenApiDocumentService(OASFilter autoSecurityFilter,
            OpenApiRecorder.UserDefinedRuntimeFilters runtimeFilters, Config config) {

        ClassLoader loader = Optional.ofNullable(OpenApiConstants.classLoader)
                .orElseGet(Thread.currentThread()::getContextClassLoader);

        try (InputStream source = loader.getResourceAsStream(OpenApiConstants.BASE_NAME + "JSON")) {
            if (source != null) {
                Set<String> userFilters = new LinkedHashSet<>(runtimeFilters.filters());
                boolean dynamic = config.getOptionalValue("quarkus.smallrye-openapi.always-run-filter", Boolean.class)
                        .orElse(Boolean.FALSE);
                SmallRyeOpenAPI.Builder builder = new OpenAPIRuntimeBuilder()
                        .withConfig(config)
                        .withApplicationClassLoader(loader)
                        .enableModelReader(false)
                        .enableStandardStaticFiles(false)
                        .enableAnnotationScan(false)
                        .enableStandardFilter(false)
                        .withCustomStaticFile(() -> source);

                // Auth-security and disabled endpoint filters will only run once
                Optional.ofNullable(autoSecurityFilter)
                        .ifPresent(builder::addFilter);
                DisabledRestEndpointsFilter.maybeGetInstance()
                        .ifPresent(builder::addFilter);

                if (dynamic && !userFilters.isEmpty()) {
                    // Only regenerate the OpenAPI document when configured and there are filters to run
                    this.documentHolder = new DynamicDocument(builder, loader, userFilters);
                } else {
                    userFilters.forEach(name -> builder.addFilter(name, loader, (IndexView) null));
                    this.documentHolder = new StaticDocument(builder.build());
                }
            } else {
                this.documentHolder = new EmptyDocument();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] getDocument(Format format) {
        if (format.equals(Format.JSON)) {
            return documentHolder.getJsonDocument();
        }
        return documentHolder.getYamlDocument();
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

        StaticDocument(SmallRyeOpenAPI openAPI) {
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
     * Generate the document on every request by re-running user-provided OASFilters.
     */
    static class DynamicDocument implements OpenApiDocumentHolder {

        private SmallRyeOpenAPI.Builder builder;

        DynamicDocument(SmallRyeOpenAPI.Builder builder, ClassLoader loader, Set<String> userFilters) {
            OpenAPI generatedOnBuild = builder.build().model();
            builder.withCustomStaticFile(() -> null);
            builder.withInitialModel(generatedOnBuild);
            userFilters.forEach(name -> builder.addFilter(name, loader, (IndexView) null));
            this.builder = builder;
        }

        public byte[] getJsonDocument() {
            return builder.build().toJSON().getBytes(StandardCharsets.UTF_8);
        }

        public byte[] getYamlDocument() {
            return builder.build().toYAML().getBytes(StandardCharsets.UTF_8);
        }
    }
}

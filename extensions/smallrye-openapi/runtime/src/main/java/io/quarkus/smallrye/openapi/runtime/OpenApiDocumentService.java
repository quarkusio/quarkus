package io.quarkus.smallrye.openapi.runtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFilter;

import io.quarkus.arc.Arc;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.runtime.filter.AutoSecurityFilter;
import io.quarkus.smallrye.openapi.runtime.filter.DisabledRestEndpointsFilter;
import io.smallrye.openapi.api.SmallRyeOpenAPI;
import io.smallrye.openapi.runtime.io.Format;

/**
 * Loads the document and make it available.
 **/
@ApplicationScoped
public class OpenApiDocumentService {

    private static final OpenApiDocumentHolder EMPTY_DOCUMENT = new EmptyDocument();

    private final Map<String, OpenApiDocumentHolder> documentHolders = new HashMap<>();

    @Inject
    Config config;

    void prepareDocument(AutoSecurityFilter autoSecurityFilter,
            Map<OpenApiFilter.RunStage, List<String>> filtersByStage, String documentName) {
        ClassLoader loader = Optional.ofNullable(OpenApiConstants.classLoader)
                .orElseGet(Thread.currentThread()::getContextClassLoader);

        Config wrappedConfig = OpenApiConfigHelper.wrap(config, documentName);

        Set<String> startupFilters = new LinkedHashSet<>(filtersByStage.get(OpenApiFilter.RunStage.RUNTIME_STARTUP));
        Set<String> requestFilters = new LinkedHashSet<>(
                filtersByStage.get(OpenApiFilter.RunStage.RUNTIME_PER_REQUEST));

        boolean hasStartup = !startupFilters.isEmpty();
        boolean hasRequest = !requestFilters.isEmpty();

        var startupFilterSetup = addFilters(startupFilters, loader);
        Consumer<SmallRyeOpenAPI.Builder> builderSetup = b -> {
            if (hasRequest) {
                // Mark the model as intermediate so that private extensions remain
                // available for filters running at each request.
                b.withIntermediateModel(true);
            }
            startupFilterSetup.accept(b);
        };

        Supplier<SmallRyeOpenAPI.Builder> builderSupplier = baseBuilderSupplier(wrappedConfig, autoSecurityFilter,
                documentName, loader, builderSetup);

        if (builderSupplier == null) {
            this.documentHolders.put(documentName, EMPTY_DOCUMENT);
            return;
        }

        OpenApiDocumentHolder holder;
        if (!hasRequest) {
            holder = new StaticDocument(hasStartup, builderSupplier);
        } else {
            holder = new DynamicDocument(hasStartup, builderSupplier, loader, wrappedConfig,
                    addFilters(requestFilters, loader));
        }
        this.documentHolders.put(documentName, holder);
    }

    private Supplier<SmallRyeOpenAPI.Builder> baseBuilderSupplier(Config wrappedConfig,
            AutoSecurityFilter autoSecurityFilter,
            String documentName, ClassLoader loader, Consumer<SmallRyeOpenAPI.Builder> builderSetup) {
        boolean isDefaultDocument = OpenApiConstants.DEFAULT_DOCUMENT_NAME.equals(documentName);
        String documentResourceName = OpenApiConstants.BASE_NAME + (isDefaultDocument ? "" : ("-" + documentName)) + ".JSON";

        byte[] documentBytes;
        try (InputStream source = loader.getResourceAsStream(documentResourceName)) {
            if (source == null) {
                return null;
            }

            documentBytes = source.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return () -> {
            SmallRyeOpenAPI.Builder builder = new OpenAPIRuntimeBuilder()
                    .withConfig(wrappedConfig)
                    .withApplicationClassLoader(loader)
                    .enableModelReader(false)
                    .enableStandardStaticFiles(false)
                    .enableAnnotationScan(false)
                    .enableStandardFilter(false)
                    .withCustomStaticFile(() -> new ByteArrayInputStream(documentBytes));

            builderSetup.accept(builder);

            // Auth-security and disabled endpoint filters will only run once
            Optional.ofNullable(autoSecurityFilter)
                    .ifPresent(builder::addFilter);
            DisabledRestEndpointsFilter.maybeGetInstance()
                    .ifPresent(builder::addFilter);
            return builder;
        };
    }

    public byte[] getDocument(String documentName, Format format) {
        OpenApiDocumentHolder holder = this.documentHolders.getOrDefault(documentName, EMPTY_DOCUMENT);

        if (format.equals(Format.JSON)) {
            return holder.getJsonDocument();
        }

        return holder.getYamlDocument();
    }

    private Consumer<SmallRyeOpenAPI.Builder> addFilters(Set<String> userFilters, ClassLoader loader) {
        return builder -> {
            for (String filterClassName : userFilters) {
                OASFilter filter = locateInstance(filterClassName, loader);

                if (filter != null) {
                    builder.addFilter(filter);
                } else {
                    builder.addFilter(filterClassName, loader, null);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private OASFilter locateInstance(String className, ClassLoader loader) {
        if (className == null) {
            return null;
        }

        Class<OASFilter> filterClass;

        try {
            filterClass = (Class<OASFilter>) loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            // Ignore it here and allow SmallRyeOpenAPI#Builder to throw OpenApiRuntimeException later
            return null;
        }

        return Arc.container().instance(filterClass).get();
    }

    /**
     * Deferred allows to lazily execute the loader. The loader is discarded after the first call. <br/>
     * In case the action happened before construction of the Deferred, an already materialized value can be provided, so that
     * the same API can be used in deferred and non defered code paths.
     */
    static class Deferred<T> {
        private final ReentrantLock lock = new ReentrantLock();
        private volatile Supplier<T> loader;

        private T materialized;

        public Deferred(Supplier<T> loader) {
            this.loader = loader;
        }

        public Deferred(T materialized) {
            this.materialized = materialized;
        }

        public T get() {
            if (this.loader != null) {
                lock.lock();
                try {
                    if (this.loader != null) {
                        materialized = loader.get();
                        loader = null;
                    }
                } finally {
                    lock.unlock();
                }
            }

            return materialized;
        }
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
     * Generate the document once when resolved by the deferred openapi.
     */
    static class StaticDocument implements OpenApiDocumentHolder {

        private record Formats(byte[] json, byte[] yaml) {
            public Formats(SmallRyeOpenAPI openAPI) {
                this(openAPI.toJSON().getBytes(StandardCharsets.UTF_8), openAPI.toYAML().getBytes(StandardCharsets.UTF_8));
            }
        }

        private final Deferred<Formats> formats;

        StaticDocument(boolean hasStartup, Supplier<SmallRyeOpenAPI.Builder> openAPISupplier) {
            if (hasStartup) {
                SmallRyeOpenAPI openAPI = openAPISupplier.get().build();
                this.formats = new Deferred<>(new Formats(openAPI));
            } else {
                this.formats = new Deferred<>(() -> {
                    SmallRyeOpenAPI openAPI = openAPISupplier.get().build();
                    return new Formats(openAPI);
                });
            }
        }

        public byte[] getJsonDocument() {
            return this.formats.get().json;
        }

        public byte[] getYamlDocument() {
            return this.formats.get().yaml;
        }
    }

    /**
     * Generate the document on every request by re-running user-provided OASFilters.
     */
    static class DynamicDocument implements OpenApiDocumentHolder {

        private final ClassLoader loader;
        private final Config config;
        private final Consumer<SmallRyeOpenAPI.Builder> filterSetup;

        private final Deferred<SmallRyeOpenAPI> baseOpenAPI;

        DynamicDocument(boolean hasStartup, Supplier<SmallRyeOpenAPI.Builder> model, ClassLoader loader, Config config,
                Consumer<SmallRyeOpenAPI.Builder> filterSetup) {
            if (hasStartup) {
                this.baseOpenAPI = new Deferred<>(model.get().build());
            } else {
                this.baseOpenAPI = new Deferred<>(() -> model.get().build());
            }

            this.loader = loader;
            this.config = config;
            this.filterSetup = filterSetup;
        }

        private SmallRyeOpenAPI build() {
            SmallRyeOpenAPI.Builder builder = new OpenAPIRuntimeBuilder()
                    .withConfig(config)
                    .withApplicationClassLoader(loader)
                    .withInitialModel(baseOpenAPI.get().model())
                    .enableModelReader(false)
                    .enableStandardStaticFiles(false)
                    .enableAnnotationScan(false)
                    .enableStandardFilter(false);

            filterSetup.accept(builder);

            return builder.build();
        }

        public byte[] getJsonDocument() {
            return build().toJSON().getBytes(StandardCharsets.UTF_8);
        }

        public byte[] getYamlDocument() {
            return build().toYAML().getBytes(StandardCharsets.UTF_8);
        }
    }
}

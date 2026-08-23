package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

/**
 * A config for the settings related to HTTP multipart request handling.
 */
public interface MultiPartConfig {
    /**
     * A comma-separated list of {@code ContentType} to indicate whether a given multipart field should be handled as a file
     * part.
     * <p>
     * You can use this setting to force HTTP-based extensions to parse a message part as a file based on its content type.
     * <p>
     * For now, this setting only works when using RESTEasy Reactive.
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> fileContentTypes();

    /**
     * The size up to which a file part is kept in memory instead of being written to the uploads directory.
     * <p>
     * With the default of {@code 0}, every file part is written to the uploads directory. Parts kept in memory
     * are reported as such by {@code FileItem#isInMemory()} and are only written to a file if their path is
     * requested.
     * <p>
     * For now, this setting only works when using Quarkus REST.
     */
    @WithDefault("0")
    MemorySize fileSizeThreshold();
}

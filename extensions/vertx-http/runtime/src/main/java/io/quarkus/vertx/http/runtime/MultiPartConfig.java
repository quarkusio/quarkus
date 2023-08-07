package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

/**
 * A {@link ConfigGroup} for the settings related to HTTP multipart request handling.
 */
public interface MultiPartConfig {
    /**
     * A comma-separated list of {@code ContentType} to indicate whether a given multipart field should be handled as a file
     * part.
     *
     * You can use this setting to force HTTP-based extensions to parse a message part as a file based on its content type.
     *
     * For now, this setting only works when using RESTEasy Reactive.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<List<String>> fileContentTypes();
}

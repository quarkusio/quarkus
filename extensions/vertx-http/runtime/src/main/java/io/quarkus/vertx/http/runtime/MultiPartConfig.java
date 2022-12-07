package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

/**
 * A {@link ConfigGroup} for the settings related to HTTP multipart request handling.
 */
@ConfigGroup
public class MultiPartConfig {

    /**
     * A list of {@code ContentType} to indicate whether a given multipart field should be handled as a file part.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> fileContentTypes;
}

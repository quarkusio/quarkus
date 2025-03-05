package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.NormalizeRootHttpPathConverter;
import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;

/**
 * @deprecated Use {@link VertxHttpBuildTimeConfig}.
 */
@Deprecated(forRemoval = true, since = "3.19")
@ConfigRoot(name = "http", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HttpBuildTimeConfig {
    /**
     * The HTTP root path. All web content will be served relative to this root path.
     */
    @ConfigItem(defaultValue = "/", generateDocumentation = false)
    @ConvertWith(NormalizeRootHttpPathConverter.class)
    public String rootPath;

    /**
     * If enabled then the response body is compressed if the {@code Content-Type} header is set and the value is a compressed
     * media type as configured via {@link #compressMediaTypes}.
     * <p>
     * Note that the RESTEasy Reactive and Reactive Routes extensions also make it possible to enable/disable compression
     * declaratively using the annotations {@link io.quarkus.vertx.http.Compressed} and
     * {@link io.quarkus.vertx.http.Uncompressed}.
     */
    @ConfigItem(generateDocumentation = false)
    public boolean enableCompression;

    /**
     * List of media types for which the compression should be enabled automatically, unless declared explicitly via
     * {@link Compressed} or {@link Uncompressed}.
     */
    @ConfigItem(defaultValue = "text/html,text/plain,text/xml,text/css,text/javascript,application/javascript,application/json,application/graphql+json,application/xhtml+xml", generateDocumentation = false)
    public Optional<List<String>> compressMediaTypes;
}

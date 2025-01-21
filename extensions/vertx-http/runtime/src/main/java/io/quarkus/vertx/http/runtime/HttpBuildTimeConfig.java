package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.NormalizeRootHttpPathConverter;

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
}

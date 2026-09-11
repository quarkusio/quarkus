package io.quarkus.test.common;

import java.nio.file.Path;

import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeInfo;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;
import io.smallrye.config.Config;

/**
 * Represents the log file used by the running integration-test artifact.
 * Backed by {@link ValueRegistry}, following the same pattern as {@link io.quarkus.vertx.http.HttpServer}.
 */
public interface TestLog {

    /** The key under which the log file path is stored in the ValueRegistry. */
    RuntimeKey<Path> LOG_FILE_PATH = RuntimeKey.key("quarkus.test.log.file.path", Path.class);

    /** The key under which the TestLog view object itself is stored. */
    RuntimeKey<TestLog> TEST_LOG = RuntimeKey.key(TestLog.class);

    /**
     * Returns the path of the log file used by this test artifact instance.
     *
     * @return the log file path, never {@code null} once the artifact has started
     */
    Path getLogFilePath();

    /**
     * Factory: constructs a {@link TestLog} view from the values already registered
     * in the given {@link ValueRegistry}.
     */
    RuntimeInfo<TestLog> INFO = new RuntimeInfo<>() {
        @Override
        public TestLog get(ValueRegistry valueRegistry) {
            return () -> {
                if (valueRegistry.containsKey(LOG_FILE_PATH)) {
                    return valueRegistry.get(LOG_FILE_PATH);
                } else {
                    LogRuntimeConfig logRuntimeConfig = Config.get().getConfigMapping(LogRuntimeConfig.class);
                    return logRuntimeConfig.file().path().toPath();
                }
            };
        }
    };
}

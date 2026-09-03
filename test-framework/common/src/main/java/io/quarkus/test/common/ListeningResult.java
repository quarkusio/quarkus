package io.quarkus.test.common;

import static io.quarkus.test.common.TestLog.LOG_FILE_PATH;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;
import io.smallrye.config.Config;

public record ListeningResult(ListeningAddress address, Path logPath) {

    public static final RuntimeKey<Optional<ListeningResult>> SERVER_LISTENING_RESULT = RuntimeKey
            .key("quarkus.http.listening-address");
    public static final RuntimeKey<Optional<ListeningResult>> MANAGEMENT_LISTENING_RESULT = RuntimeKey
            .key("quarkus.management.listening-address");

    public void register(ValueRegistry valueRegistry, Config config) {
        address.register(valueRegistry, config);
        valueRegistry.register(LOG_FILE_PATH, logPath);
    }

    public void registerManagement(ValueRegistry valueRegistry, Config config) {
        address.registerManagement(valueRegistry, config);
    }
}

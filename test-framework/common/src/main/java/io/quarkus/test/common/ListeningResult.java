package io.quarkus.test.common;

import static io.quarkus.test.common.TestLog.LOG_FILE_PATH;
import static io.quarkus.test.common.TestLog.TEST_LOG;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;
import io.smallrye.config.Config;

public record ListeningResult(Integer port, String protocol, Path logPath) {

    public static final RuntimeKey<Optional<ListeningResult>> LISTENING_ADDRESS = RuntimeKey
            .key("quarkus.http.listening-address");

    public void register(ValueRegistry valueRegistry, Config config) {
        ListeningAddress address = new ListeningAddress(port, protocol);
        address.register(valueRegistry, config);
        valueRegistry.register(LOG_FILE_PATH, logPath);
        valueRegistry.registerInfo(TEST_LOG, TestLog.INFO);
    }
}

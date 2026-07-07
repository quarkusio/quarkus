package io.quarkus.it.vertx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class UnixDomainSocketTestResource implements QuarkusTestResourceLifecycleManager {

    private Path udsPath;

    @Override
    public void init(Map<String, String> initArgs) {
        try {
            udsPath = Files.createTempFile(Path.of("/tmp"), "quarkus-test", ".sock");
            Files.delete(udsPath); // Let Quarkus create a proper UDS with same path
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> start() {
        return Map.of(
                "quarkus.http.domain-socket-enabled", "true",
                "quarkus.http.domain-socket", udsPath.toAbsolutePath().toString(),
                "quarkus.http.host-enabled", "false",
                "quarkus.vertx.native-transport", "disabled");
    }

    @Override
    public void stop() {
        try {
            Files.deleteIfExists(udsPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

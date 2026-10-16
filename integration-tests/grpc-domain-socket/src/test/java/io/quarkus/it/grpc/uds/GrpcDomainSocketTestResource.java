package io.quarkus.it.grpc.uds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class GrpcDomainSocketTestResource implements QuarkusTestResourceLifecycleManager {

    private Path socketPath;

    @Override
    public void init(Map<String, String> initArgs) {
        try {
            socketPath = Files.createTempFile(Path.of("/tmp"), "quarkus-grpc-test", ".sock");
            Files.delete(socketPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> start() {
        String path = socketPath.toAbsolutePath().toString();
        return Map.of(
                "quarkus.http.domain-socket-enabled", "true",
                "quarkus.http.domain-socket", path,
                "quarkus.http.host-enabled", "false",
                "quarkus.vertx.native-transport", "disabled",
                "quarkus.grpc.clients.hello.domain-socket", path);
    }

    @Override
    public void stop() {
        try {
            Files.deleteIfExists(socketPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

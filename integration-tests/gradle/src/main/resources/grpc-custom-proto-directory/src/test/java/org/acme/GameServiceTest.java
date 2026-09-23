package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.acme.protocol.GameService;
import org.acme.protocol.HelloRequest;
import org.acme.protocol.HelloResponse;
import org.junit.jupiter.api.Test;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GameServiceTest {

    @GrpcClient("game-service")
    GameService gameService;

    @Test
    void testHello() {
        HelloRequest request = HelloRequest.newBuilder().build();
        HelloResponse response = gameService.hello(request).await().indefinitely();

        assertNotNull(response);
        assertEquals("Hello from gRPC service!", response.getMessage());
    }
}

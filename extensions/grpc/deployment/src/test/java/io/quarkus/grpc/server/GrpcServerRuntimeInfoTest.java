package io.quarkus.grpc.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.runtime.GrpcServer;
import io.quarkus.test.QuarkusUnitTest;

public class GrpcServerRuntimeInfoTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Inject
    GrpcServer server;

    @Test
    void ports() {
        assertTrue(server.getPort() > 0);
    }

    @GrpcService
    static class FooService implements BindableService {
        @Override
        public ServerServiceDefinition bindService() {
            return ServerServiceDefinition.builder("foo").build();
        }
    }
}

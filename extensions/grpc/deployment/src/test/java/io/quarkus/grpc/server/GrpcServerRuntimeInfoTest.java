package io.quarkus.grpc.server;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.Descriptors;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.protobuf.ProtoServiceDescriptorSupplier;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.runtime.GrpcServer;
import io.quarkus.test.QuarkusExtensionTest;

public class GrpcServerRuntimeInfoTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest();

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
            var mock = mock(ServerServiceDefinition.class);
            var sd = mock(ServiceDescriptor.class);
            when(sd.getName()).thenReturn("service1");
            when(sd.getSchemaDescriptor()).thenReturn(new ProtoServiceDescriptorSupplier() {
                @Override
                public Descriptors.ServiceDescriptor getServiceDescriptor() {
                    return mock(Descriptors.ServiceDescriptor.class);
                }

                @Override
                public Descriptors.FileDescriptor getFileDescriptor() {
                    return mock(Descriptors.FileDescriptor.class);
                }
            });
            when(mock.getServiceDescriptor()).thenReturn(sd);
            return mock;
        }
    }
}

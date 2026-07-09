package io.quarkus.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.Descriptors;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.protobuf.ProtoServiceDescriptorSupplier;
import io.quarkus.arc.Arc;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.runtime.GrpcContainer;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verify that no gRPC server is started when {@code quarkus.grpc.server.enabled} is set to {@code false},
 * even if the application exposes gRPC services.
 */
public class GrpcServerDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyFakeService.class))
            .overrideConfigKey("quarkus.grpc.server.enabled", "false");

    @Test
    public void test() {
        // The GrpcContainer bean backing the gRPC server must not be registered when the server is disabled
        assertThat(Arc.container().instance(GrpcContainer.class).isAvailable()).isFalse();
    }

    @GrpcService
    static class MyFakeService implements BindableService {

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

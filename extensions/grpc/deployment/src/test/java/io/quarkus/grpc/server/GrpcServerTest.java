package io.quarkus.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Collectors;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.Descriptors;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.protobuf.ProtoServiceDescriptorSupplier;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.QuarkusExtensionTest;

public class GrpcServerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyFakeService.class, MySecondFakeService.class))
            .withConfigurationResource("grpc-server-no-health-configuration.properties");

    @Inject
    @Any
    Instance<BindableService> services;

    @Test
    public void test() {
        assertThat(services.stream().collect(Collectors.toList())).hasSize(2)
                .anySatisfy(b -> assertThat(b.bindService().getServiceDescriptor().getName()).isEqualTo("service1"))
                .anySatisfy(b -> assertThat(b.bindService().getServiceDescriptor().getName()).isEqualTo("service2"));
    }

    // On the usage of Mockito
    // with Vert.x 5, there is a lot more checks done in the bridge that makes it impossible to juste use
    // "fake" BindableServices. We have to use mocks to workaround the checks.

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

    @GrpcService
    static class MySecondFakeService implements BindableService {

        @Override
        public ServerServiceDefinition bindService() {
            var mock = mock(ServerServiceDefinition.class);
            var sd = mock(ServiceDescriptor.class);
            when(sd.getName()).thenReturn("service2");
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

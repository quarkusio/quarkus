package io.quarkus.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.runtime.GrpcServerRecorder;
import io.quarkus.test.QuarkusUnitTest;

public class GrpcServerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
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
        assertThat(GrpcServerRecorder.getVerticleCount()).isGreaterThan(0);
    }

    @GrpcService
    static class MyFakeService implements BindableService {

        @Override
        public ServerServiceDefinition bindService() {
            return ServerServiceDefinition.builder("service1").build();
        }
    }

    @GrpcService
    static class MySecondFakeService implements BindableService {

        @Override
        public ServerServiceDefinition bindService() {
            return ServerServiceDefinition.builder("service2").build();
        }
    }

}

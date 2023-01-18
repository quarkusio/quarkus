package io.quarkus.grpc.server;

import static java.util.Arrays.asList;
import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import grpc.health.v1.HealthGrpc;
import grpc.health.v1.HealthOuterClass;
import grpc.health.v1.HealthOuterClass.HealthCheckResponse;
import grpc.health.v1.HealthOuterClass.HealthCheckResponse.ServingStatus;
import grpc.health.v1.MutinyHealthGrpc;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class GrpcHealthTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClass(HelloService.class))
            .withConfigurationResource("health-config.properties");

    @Inject
    GrpcHealthStorage healthService;

    @Inject
    HealthConsumer healthConsumer;

    @Test
    public void shouldGetDefaultHealthInfo() {
        ServingStatus defaultStatus = healthConsumer.checkStatus(Function.identity());
        assertThat(defaultStatus).isEqualTo(ServingStatus.SERVING);
    }

    @Test
    public void shouldGetHealthServiceHealthInfo() {
        ServingStatus defaultStatus = healthConsumer.checkStatus(builder -> builder.setService("grpc.health.v1.Health"));
        assertThat(defaultStatus).isEqualTo(ServingStatus.SERVING);
    }

    @Test
    public void shouldGetCustomHealthInfo() {
        String customService = "customService";
        healthService.setStatus(customService, ServingStatus.NOT_SERVING);
        ServingStatus defaultStatus = healthConsumer.checkStatus(builder -> builder.setService(customService));
        assertThat(defaultStatus).isEqualTo(ServingStatus.NOT_SERVING);
    }

    @Test
    public void shouldGetHealthInfoFromMultipleStreams() {
        String serviceName = "customService2";

        List<ServingStatus> statusList1 = new CopyOnWriteArrayList<>();
        List<ServingStatus> statusList2 = new CopyOnWriteArrayList<>();
        List<ServingStatus> statusList3 = new CopyOnWriteArrayList<>();

        gatherStatusesToList(serviceName, statusList1);
        gatherStatusesToList(serviceName, statusList2);

        // to check if the unknown was delivered and we are attached
        awaitExactStatusList(statusList1, ServingStatus.UNKNOWN);
        awaitExactStatusList(statusList2, ServingStatus.UNKNOWN);

        healthService.setStatus(serviceName, ServingStatus.NOT_SERVING);

        gatherStatusesToList(serviceName, statusList3);

        awaitExactStatusList(statusList3, ServingStatus.NOT_SERVING);

        healthService.setStatus(serviceName, ServingStatus.SERVING);
        healthService.setStatus(serviceName, ServingStatus.NOT_SERVING);

        awaitExactStatusList(statusList1, ServingStatus.UNKNOWN, ServingStatus.NOT_SERVING, ServingStatus.SERVING,
                ServingStatus.NOT_SERVING);
        awaitExactStatusList(statusList2, ServingStatus.UNKNOWN, ServingStatus.NOT_SERVING, ServingStatus.SERVING,
                ServingStatus.NOT_SERVING);
        awaitExactStatusList(statusList3, ServingStatus.NOT_SERVING, ServingStatus.SERVING, ServingStatus.NOT_SERVING);
    }

    private void gatherStatusesToList(String serviceName, List<ServingStatus> statusList1) {
        healthConsumer.getStatusStream(builder -> builder.setService(serviceName))
                .map(HealthCheckResponse::getStatus).subscribe().with(statusList1::add);
    }

    private void awaitExactStatusList(List<ServingStatus> statusList, ServingStatus... statuses) {
        await("list to be contain exactly " + Arrays.toString(statuses)).atMost(2, TimeUnit.SECONDS)
                .until(() -> statusList, isEqual(asList(statuses)));
    }

    @ApplicationScoped
    static class HealthConsumer {

        @GrpcClient("health-service")
        HealthGrpc.HealthBlockingStub healthBlocking;

        @GrpcClient("health-service")
        MutinyHealthGrpc.MutinyHealthStub healthMutiny;

        public HealthCheckResponse.ServingStatus checkStatus(
                Function<HealthOuterClass.HealthCheckRequest.Builder, HealthOuterClass.HealthCheckRequest.Builder> decorator) {
            HealthOuterClass.HealthCheckRequest.Builder builder = decorator
                    .apply(HealthOuterClass.HealthCheckRequest.newBuilder());
            return healthBlocking.check(builder.build()).getStatus();
        }

        public Multi<HealthCheckResponse> getStatusStream(
                Function<HealthOuterClass.HealthCheckRequest.Builder, HealthOuterClass.HealthCheckRequest.Builder> decorator) {
            HealthOuterClass.HealthCheckRequest.Builder builder = decorator
                    .apply(HealthOuterClass.HealthCheckRequest.newBuilder());
            return healthMutiny.watch(builder.build());
        }
    }
}

package io.quarkus.grpc.health;

import static io.restassured.RestAssured.when;
import static java.util.Arrays.asList;
import static java.util.function.Predicate.isEqual;
import static org.awaitility.Awaitility.await;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import grpc.health.v1.HealthGrpc;
import grpc.health.v1.HealthOuterClass;
import grpc.health.v1.HealthOuterClass.HealthCheckResponse.ServingStatus;
import grpc.health.v1.MutinyHealthGrpc;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

/*
 * MP Health info for gRPC looks as follows (the data section contains entries for each of the available services):
 * {
    "status": "UP",
    "checks": [
        {
            "name": "gRPC Server",
            "status": "UP",
            "data": {
                "grpc.health.v1.Health": true
            }
        }
    ]
}
 */
public class MicroProfileHealthEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(HealthGrpc.class.getPackage())
                            .addClass(FakeService.class))
            .withConfigurationResource("health-config.properties");

    @Inject
    HealthConsumer healthConsumer;

    @Test
    public void shouldNotFailMpHealthCheckOnGettingUnknownServiceStatus() {
        checkHealthOk();
        askForHealthOfUnknownService();
        checkHealthOk();
    }

    private void checkHealthOk() {
        // @formatter:off
        when()
                .get("/q/health")
        .then()
                .statusCode(200)
                .body("checks[0].name", Matchers.equalTo("gRPC Server"))
                .body("checks[0].status", Matchers.equalTo("UP"))
                .body("checks[0].data['grpc.health.v1.Health']", Matchers.equalTo(true))
                .body("checks[0].data.size()", Matchers.equalTo(2));
        // @formatter:on
    }

    private void askForHealthOfUnknownService() {
        String serviceName = "customService2";
        List<ServingStatus> statusList = new CopyOnWriteArrayList<>();
        gatherStatusesToList(serviceName, statusList);

        // to check if the unknown was delivered and we are attached
        awaitExactStatusList(statusList, ServingStatus.UNKNOWN);
    }

    private void gatherStatusesToList(String serviceName, List<ServingStatus> statusList1) {
        healthConsumer.getStatusStream(builder -> builder.setService(serviceName))
                .map(HealthOuterClass.HealthCheckResponse::getStatus).subscribe().with(statusList1::add);
    }

    private void awaitExactStatusList(List<ServingStatus> statusList, ServingStatus... statuses) {
        await("list to be contain exactly " + Arrays.toString(statuses)).atMost(2, TimeUnit.SECONDS)
                .until(() -> statusList, isEqual(asList(statuses)));
    }

    @ApplicationScoped
    static class HealthConsumer {

        @GrpcClient("health-service")
        MutinyHealthGrpc.MutinyHealthStub healthMutiny;

        public Multi<HealthOuterClass.HealthCheckResponse> getStatusStream(
                Function<HealthOuterClass.HealthCheckRequest.Builder, HealthOuterClass.HealthCheckRequest.Builder> decorator) {
            HealthOuterClass.HealthCheckRequest.Builder builder = decorator
                    .apply(HealthOuterClass.HealthCheckRequest.newBuilder());
            return healthMutiny.watch(builder.build());
        }
    }

    @GrpcService
    public static class FakeService implements BindableService {

        @Override
        public ServerServiceDefinition bindService() {
            return ServerServiceDefinition.builder("fake").build();
        }
    }
}

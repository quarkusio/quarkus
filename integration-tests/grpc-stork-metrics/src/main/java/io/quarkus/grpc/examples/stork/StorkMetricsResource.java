package io.quarkus.grpc.examples.stork;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.micrometer.runtime.binder.stork.StorkObservationCollectorBean;
import io.smallrye.stork.api.observability.StorkObservation;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class StorkMetricsResource {

    private static final String SERVICE_NAME = "hello-service";

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub blockingStub;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutinyStub;

    @Inject
    MeterRegistry registry;

    void init(@Observes Router router) {
        router.get("/call/blocking").blockingHandler(rc -> {
            String name = rc.queryParam("name").stream().findFirst().orElse("blocking");
            HelloReply reply = blockingStub.sayHello(HelloRequest.newBuilder().setName(name).build());
            rc.response().end(reply.getMessage());
        });

        router.get("/call/mutiny").blockingHandler(rc -> {
            String name = rc.queryParam("name").stream().findFirst().orElse("mutiny");
            HelloReply reply = mutinyStub.sayHello(HelloRequest.newBuilder().setName(name).build())
                    .await().atMost(Duration.ofSeconds(5));
            rc.response().end(reply.getMessage());
        });

        router.get("/stork-metrics-snapshot").blockingHandler(rc -> {
            Counter instanceCounter = registry.find("stork.service-discovery.instances.count")
                    .tags("service-name", SERVICE_NAME)
                    .counter();
            Timer serviceDiscoveryDuration = registry.find("stork.service-discovery.duration")
                    .tags("service-name", SERVICE_NAME)
                    .timer();
            Timer serviceSelectionDuration = registry.find("stork.service-selection.duration")
                    .tags("service-name", SERVICE_NAME)
                    .timer();
            Counter serviceDiscoveryFailures = registry.find("stork.service-discovery.failures")
                    .tags("service-name", SERVICE_NAME)
                    .counter();
            Counter serviceSelectionFailures = registry.find("stork.service-selection.failures")
                    .tags("service-name", SERVICE_NAME)
                    .counter();

            StorkObservation metrics = StorkObservationCollectorBean.STORK_METRICS
                    .get(SERVICE_NAME + StorkObservationCollectorBean.METRICS_SUFFIX);

            StringBuilder sb = new StringBuilder();
            sb.append("instanceCounter.present=").append(instanceCounter != null).append('\n');
            sb.append("serviceDiscoveryDuration.present=").append(serviceDiscoveryDuration != null).append('\n');
            sb.append("serviceSelectionDuration.present=").append(serviceSelectionDuration != null).append('\n');
            sb.append("serviceDiscoveryFailures.present=").append(serviceDiscoveryFailures != null).append('\n');
            sb.append("serviceSelectionFailures.present=").append(serviceSelectionFailures != null).append('\n');
            if (instanceCounter != null) {
                sb.append("instanceCounter.count=").append(instanceCounter.count()).append('\n');
            }
            if (serviceDiscoveryDuration != null) {
                sb.append("serviceDiscoveryDuration.totalTimeNanos=")
                        .append(serviceDiscoveryDuration.totalTime(TimeUnit.NANOSECONDS)).append('\n');
            }
            if (serviceSelectionDuration != null) {
                sb.append("serviceSelectionDuration.totalTimeNanos=")
                        .append(serviceSelectionDuration.totalTime(TimeUnit.NANOSECONDS)).append('\n');
            }
            if (serviceDiscoveryFailures != null) {
                sb.append("serviceDiscoveryFailures.count=").append(serviceDiscoveryFailures.count()).append('\n');
            }
            if (serviceSelectionFailures != null) {
                sb.append("serviceSelectionFailures.count=").append(serviceSelectionFailures.count()).append('\n');
            }
            sb.append("observation.present=").append(metrics != null).append('\n');
            if (metrics != null) {
                sb.append("observation.discoveredInstancesCount=").append(metrics.getDiscoveredInstancesCount()).append('\n');
                sb.append("observation.serviceName=").append(metrics.getServiceName()).append('\n');
                sb.append("observation.done=").append(metrics.isDone()).append('\n');
                sb.append("observation.failureNull=").append(metrics.failure() == null).append('\n');
                sb.append("observation.serviceDiscoveryType=").append(metrics.getServiceDiscoveryType()).append('\n');
                sb.append("observation.serviceSelectionType=").append(metrics.getServiceSelectionType()).append('\n');
                sb.append("observation.serviceDiscoveryDurationPresent=")
                        .append(metrics.getServiceDiscoveryDuration() != null).append('\n');
                sb.append("observation.serviceSelectionDurationPresent=")
                        .append(metrics.getServiceSelectionDuration() != null).append('\n');
            }
            rc.response().putHeader("content-type", "text/plain").end(sb.toString());
        });
    }
}

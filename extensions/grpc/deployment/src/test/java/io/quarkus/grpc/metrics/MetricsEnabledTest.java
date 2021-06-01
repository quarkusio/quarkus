package io.quarkus.grpc.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloRequest;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.Search;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;

public class MetricsEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage()).addPackage(Greeter.class.getPackage())
                    .addClasses(HelloService.class))
            .withConfigurationResource("hello-metrics-enabled.properties");

    @GrpcClient("hello-service")
    Greeter service;

    @Inject
    MeterRegistry meterRegistry;

    @Test
    public void testMetrics() {
        service.sayHello(HelloRequest.newBuilder().setName("Foo").build()).await().atMost(Duration.ofSeconds(5));
        Meter requestsReceived = Search.in(meterRegistry).name("grpc.server.requests.received").meter();
        assertNotNull(requestsReceived);
        assertEquals(Type.COUNTER, requestsReceived.getId().getType());
        List<Tag> tags = requestsReceived.getId().getTags();
        // service, method, methodType
        assertEquals(3, tags.size());
    }

}

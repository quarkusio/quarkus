
package io.quarkus.micrometer.deployment.binder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.quarkus.micrometer.runtime.binder.stork.StorkObservationCollectorBean;
import io.quarkus.micrometer.test.GreetingResource;
import io.quarkus.micrometer.test.MockServiceSelectorConfiguration;
import io.quarkus.micrometer.test.MockServiceSelectorProvider;
import io.quarkus.micrometer.test.MockServiceSelectorProviderLoader;
import io.quarkus.micrometer.test.PingPongResource;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.stork.api.observability.StorkObservation;

@DisabledOnOs(OS.WINDOWS)
public class StorkMetricsLoadBalancerFailTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("pingpong/mp-rest/url", "stork://pingpong-service")
            .overrideConfigKey("quarkus.stork.pingpong-service.service-discovery.type", "static")
            .overrideConfigKey("quarkus.stork.pingpong-service.service-discovery.address-list", "${test.url}")
            .overrideConfigKey("quarkus.stork.pingpong-service.load-balancer.type", "mock")
            .overrideConfigKey("greeting/mp-rest/url", "stork://greeting-service/greeting")
            .overrideConfigKey("quarkus.stork.greeting-service.service-discovery.type", "static")
            .overrideConfigKey("quarkus.stork.greeting-service.service-discovery.address-list", "${test.url}")
            .overrideConfigKey("quarkus.stork.greeting-service.load-balancer.type", "mock")
            .withApplicationRoot((jar) -> jar
                    .addClasses(PingPongResource.class, PingPongResource.PingPongRestClient.class,
                            MockServiceSelectorProvider.class, MockServiceSelectorConfiguration.class,
                            MockServiceSelectorProviderLoader.class, GreetingResource.class,
                            GreetingResource.GreetingRestClient.class, Util.class));

    @Inject
    MeterRegistry registry;

    @Inject
    MockServiceSelectorProvider provider;

    @Test
    public void shouldGetStorkMetricsWhenServiceSelectorFails() {

        Mockito.when(provider.getLoadBalancer().selectServiceInstance(Mockito.anyCollection()))
                .thenThrow(new RuntimeException("Load Balancer induced failure"));
        RestAssured.when().get("/ping/one").then().statusCode(500);
        RestAssured.when().get("/greeting/hola").then().statusCode(500);

        //Stork metrics
        assertStorkMetrics("pingpong-service");
        assertStorkMetrics("greeting-service");

        // Stork metrics exposed to Micrometer
        assertStorkMetricsInMicrometerRegistry("pingpong-service");
        assertStorkMetricsInMicrometerRegistry("greeting-service");

    }

    private static void assertStorkMetrics(String serviceName) {
        StorkObservation metrics = StorkObservationCollectorBean.STORK_METRICS
                .get(serviceName + StorkObservationCollectorBean.METRICS_SUFIX);
        Assertions.assertThat(metrics.getDiscoveredInstancesCount()).isEqualTo(1);
        Assertions.assertThat(metrics.getServiceName()).isEqualTo(serviceName);
        Assertions.assertThat(metrics.isDone()).isTrue();
        Assertions.assertThat(metrics.isServiceDiscoverySuccessful()).isTrue();
        Assertions.assertThat(metrics.failure().getMessage())
                .isEqualTo("Load Balancer induced failure");
        Assertions.assertThat(metrics.getOverallDuration()).isNotNull();
        Assertions.assertThat(metrics.getServiceDiscoveryType()).isEqualTo("static");
        Assertions.assertThat(metrics.getServiceSelectionType()).isEqualTo("mock");
        Assertions.assertThat(metrics.getServiceDiscoveryDuration()).isNotNull();
        Assertions.assertThat(metrics.getServiceSelectionDuration()).isNotNull();
    }

    private void assertStorkMetricsInMicrometerRegistry(String serviceName) {
        Counter instanceCounter = registry.counter("stork.service-discovery.instances.count", "service-name", serviceName);
        Timer serviceDiscoveryDuration = registry.timer("stork.service-discovery.duration", "service-name", serviceName);
        Timer serviceSelectionDuration = registry.timer("stork.service-selection.duration", "service-name", serviceName);
        Counter serviceDiscoveryFailures = registry.get("stork.service-discovery.failures")
                .tags("service-name", serviceName).counter();
        Counter loadBalancerFailures = registry.get("stork.service-selection.failures").tags("service-name", serviceName)
                .counter();

        Util.assertTags(Tag.of("service-name", serviceName), instanceCounter, serviceDiscoveryDuration,
                serviceSelectionDuration);

        Assertions.assertThat(instanceCounter.count()).isEqualTo(1);
        Assertions.assertThat(loadBalancerFailures.count()).isEqualTo(1);
        Assertions.assertThat(serviceDiscoveryFailures.count()).isEqualTo(0);
        Assertions.assertThat(serviceDiscoveryDuration.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);
        Assertions.assertThat(serviceSelectionDuration.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);
    }

}

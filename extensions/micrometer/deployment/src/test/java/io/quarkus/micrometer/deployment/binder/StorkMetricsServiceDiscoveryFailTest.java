
package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.runtime.binder.stork.StorkObservationCollectorBean;
import io.quarkus.micrometer.test.GreetingResource;
import io.quarkus.micrometer.test.MockServiceDiscoveryConfiguration;
import io.quarkus.micrometer.test.MockServiceDiscoveryProvider;
import io.quarkus.micrometer.test.MockServiceDiscoveryProviderLoader;
import io.quarkus.micrometer.test.PingPongResource;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.observability.StorkObservation;

@DisabledOnOs(OS.WINDOWS)
public class StorkMetricsServiceDiscoveryFailTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.stork.pingpong-service.service-discovery.type", "mock")
            .overrideConfigKey("pingpong/mp-rest/url", "stork://pingpong-service")
            .overrideConfigKey("greeting/mp-rest/url", "stork://greeting-service/greeting")
            .overrideConfigKey("quarkus.stork.greeting-service.service-discovery.type", "mock")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar
                    .addClasses(PingPongResource.class, PingPongResource.PingPongRestClient.class,
                            MockServiceDiscoveryProvider.class, MockServiceDiscoveryConfiguration.class,
                            MockServiceDiscoveryProviderLoader.class, GreetingResource.class,
                            GreetingResource.GreetingRestClient.class, Util.class));

    final static SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeAll
    static void setRegistry() {
        Metrics.addRegistry(registry);
    }

    @AfterAll()
    static void removeRegistry() {
        Metrics.removeRegistry(registry);
    }

    @Inject
    MockServiceDiscoveryProvider provider;

    @Test
    public void shouldGetStorkMetricsWhenServiceDiscoveryFails() {

        Mockito.when(provider.getServiceDiscovery().getServiceInstances())
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Service Discovery induced failure")));
        RestAssured.when().get("/ping/one").then().statusCode(500);
        when().get("/greeting/hola").then().statusCode(500);

        //Stork metrics
        assertStorkMetrics("pingpong-service");
        assertStorkMetrics("greeting-service");

        // Stork metrics exposed to Micrometer
        assertStorkMetricsInMicrometerRegistry("pingpong-service");
        assertStorkMetricsInMicrometerRegistry("greeting-service");

    }

    private void assertStorkMetricsInMicrometerRegistry(String serviceName) {
        Counter instanceCounter = registry.find("stork.service-discovery.instances.count").tags("service-name", serviceName)
                .counter();
        Timer serviceDiscoveryDuration = registry.find("stork.service-discovery.duration").tags("service-name", serviceName)
                .timer();
        Timer serviceSelectionDuration = registry.find("stork.service-selection.duration").tags("service-name", serviceName)
                .timer();
        Counter serviceDiscoveryFailures = registry.find("stork.service-discovery.failures").tags("service-name", serviceName)
                .counter();
        Counter loadBalancerFailures = registry.find("stork.service-selection.failures").tags("service-name", serviceName)
                .counter();

        Util.assertTags(Tag.of("service-name", serviceName), instanceCounter, serviceDiscoveryDuration,
                serviceSelectionDuration);

        Assertions.assertThat(instanceCounter).isNotNull();
        Assertions.assertThat(serviceDiscoveryDuration).isNotNull();
        Assertions.assertThat(serviceSelectionDuration).isNotNull();
        Assertions.assertThat(serviceDiscoveryFailures).isNotNull();
        Assertions.assertThat(loadBalancerFailures).isNotNull();
        Assertions.assertThat(instanceCounter.count()).isEqualTo(0);
        Assertions.assertThat(loadBalancerFailures.count()).isEqualTo(0);
        Assertions.assertThat(serviceDiscoveryFailures.count()).isEqualTo(1);
        Assertions.assertThat(serviceDiscoveryDuration.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);
        Assertions.assertThat(serviceSelectionDuration.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);
    }

    private static void assertStorkMetrics(String serviceName) {
        StorkObservation metrics = StorkObservationCollectorBean.STORK_METRICS
                .get(serviceName + StorkObservationCollectorBean.METRICS_SUFFIX);
        Assertions.assertThat(metrics.getDiscoveredInstancesCount()).isNegative();
        Assertions.assertThat(metrics.getServiceName()).isEqualTo(serviceName);
        Assertions.assertThat(metrics.isDone()).isTrue();
        Assertions.assertThat(metrics.isServiceDiscoverySuccessful()).isFalse();
        Assertions.assertThat(metrics.failure().getMessage())
                .isEqualTo("Service Discovery induced failure");
        Assertions.assertThat(metrics.getOverallDuration()).isNotNull();
        Assertions.assertThat(metrics.getServiceDiscoveryType()).isEqualTo("mock");
        Assertions.assertThat(metrics.getServiceSelectionType()).isEqualTo("round-robin");
        Assertions.assertThat(metrics.getServiceDiscoveryDuration()).isNotNull();
        Assertions.assertThat(metrics.getServiceSelectionDuration()).isNotNull();
    }

}


package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.runtime.binder.stork.StorkObservationCollectorBean;
import io.quarkus.micrometer.test.GreetingResource;
import io.quarkus.micrometer.test.PingPongResource;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.stork.api.observability.StorkObservation;

@DisabledOnOs(OS.WINDOWS)
public class StorkMetricsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("pingpong/mp-rest/url", "stork://pingpong-service")
            .overrideConfigKey("quarkus.stork.pingpong-service.service-discovery.type", "static")
            .overrideConfigKey("quarkus.stork.pingpong-service.service-discovery.address-list", "${test.url}")
            .overrideConfigKey("greeting/mp-rest/url", "stork://greeting-service/greeting")
            .overrideConfigKey("quarkus.stork.greeting-service.service-discovery.type", "static")
            .overrideConfigKey("quarkus.stork.greeting-service.service-discovery.address-list", "${test.url}")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.log.category.\"io.micrometer.core.instrument\".level", "DEBUG")
            .withApplicationRoot((jar) -> jar
                    .addClasses(PingPongResource.class, PingPongResource.PingPongRestClient.class, GreetingResource.class,
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

    @Test
    public void shouldGetStorkMetricsForTwoServicesWhenEverythingSucceeded() {
        when().get("/ping/one").then().statusCode(200);
        when().get("greeting/hola").then().statusCode(200);

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
        Assertions.assertThat(instanceCounter.count()).isEqualTo(1);
        Assertions.assertThat(serviceDiscoveryDuration.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);
        Assertions.assertThat(serviceSelectionDuration.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);
        Assertions.assertThat(serviceDiscoveryFailures.count()).isEqualTo(0);
        Assertions.assertThat(loadBalancerFailures.count()).isEqualTo(0);
    }

    public static void assertStorkMetrics(String serviceName) {
        StorkObservation metrics = StorkObservationCollectorBean.STORK_METRICS
                .get(serviceName + StorkObservationCollectorBean.METRICS_SUFFIX);

        Assertions.assertThat(metrics.getDiscoveredInstancesCount()).isEqualTo(1);
        Assertions.assertThat(metrics.getServiceName()).isEqualTo(serviceName);
        Assertions.assertThat(metrics.isDone()).isTrue();
        Assertions.assertThat(metrics.failure()).isNull();
        Assertions.assertThat(metrics.getOverallDuration()).isNotNull();
        Assertions.assertThat(metrics.getServiceDiscoveryType()).isEqualTo("static");
        Assertions.assertThat(metrics.getServiceSelectionType()).isEqualTo("round-robin");
        Assertions.assertThat(metrics.getServiceDiscoveryDuration()).isNotNull();
        Assertions.assertThat(metrics.getServiceSelectionDuration()).isNotNull();
    }

}

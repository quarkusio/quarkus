package io.quarkus.stork;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

public class StorkConfigUtilTest {

    private static Config config;

    @BeforeAll
    static void setup() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("quarkus.http.host", "localhost");
        configMap.put("quarkus.http.port", "9090");

        config = new SmallRyeConfigBuilder()
                .withSources(new MapBackedConfigSource("test", configMap) {
                })
                .build();
    }

    @AfterEach
    void tearDown() {
        ConfigProviderResolver.instance().releaseConfig(config);
    }

    @Test
    public void shouldBuildDefaultRegistrarConfigurationWithHealthCheck() {
        String healthCheckPath = "/q/health";
        String registrarType = "consul";

        ServiceConfiguration config = StorkConfigUtil.buildDefaultRegistrarConfiguration(registrarType, healthCheckPath);

        assertThat(config.serviceRegistrar().isPresent()).isTrue();
        var registrar = config.serviceRegistrar().get();

        assertThat(registrar.type().isPresent()).isTrue();
        assertThat(registrarType).isEqualTo(registrar.type().get());
        Map<String, String> parameters = registrar.parameters();
        assertThat(parameters).hasSize(1);
        assertThat(parameters).containsKey("health-check-url");

        String hcUrl = parameters.get("health-check-url");
        assertThat(hcUrl).contains("/q/health");
    }

    @Test
    public void shouldBuildDefaultRegistrarConfigurationWithoutHealthCheck() {
        String healthCheckPath = "";
        String registrarType = "consul";

        ServiceConfiguration config = StorkConfigUtil.buildDefaultRegistrarConfiguration(registrarType, healthCheckPath);

        assertThat(config.serviceRegistrar().isPresent()).isTrue();
        var registrar = config.serviceRegistrar().get();

        assertThat(registrar.type().isPresent()).isTrue();
        assertThat(registrarType).isEqualTo(registrar.type().get());
        Map<String, String> parameters = registrar.parameters();
        assertThat(parameters).hasSize(0);
    }

    @Test
    public void shouldFailsBecauseEmptyRegistrarType() {
        String healthCheckPath = "";
        String registrarType = "";

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StorkConfigUtil.buildDefaultRegistrarConfiguration(registrarType, healthCheckPath);
        });

        String expectedMessage = "Parameter type should be provided.";
        String actualMessage = exception.getMessage();

        assertThat(actualMessage).contains(expectedMessage);

    }

    @Test
    public void shouldAddRegistrarTypeGivenAnEmptyConfig() {
        // Given an empty registration config
        ServiceConfiguration original = new ServiceConfiguration() {
            @Override
            public Optional<StorkServiceDiscoveryConfiguration> serviceDiscovery() {
                return Optional.empty();
            }

            @Override
            public StorkLoadBalancerConfiguration loadBalancer() {
                return null;
            }

            @Override
            public Optional<StorkServiceRegistrarConfiguration> serviceRegistrar() {
                return Optional.empty();
            }
        };

        ServiceConfiguration updated = StorkConfigUtil.addRegistrarTypeIfAbsent("consul", original, "/custom");

        assertThat(updated.serviceRegistrar()).isPresent();
        var registrar = updated.serviceRegistrar().get();

        assertThat(registrar.type()).isPresent();
        assertThat("consul").isEqualTo(registrar.type().get());
        assertThat("/custom").isEqualTo(registrar.parameters().get("health-check-url"));
    }

    @Test
    public void shouldFailWhenAddingEmptyRegistrarTypeGivenAnEmptyConfig() {
        // Given an empty registration config
        ServiceConfiguration original = new ServiceConfiguration() {
            @Override
            public Optional<StorkServiceDiscoveryConfiguration> serviceDiscovery() {
                return Optional.empty();
            }

            @Override
            public StorkLoadBalancerConfiguration loadBalancer() {
                return null;
            }

            @Override
            public Optional<StorkServiceRegistrarConfiguration> serviceRegistrar() {
                return Optional.empty();
            }
        };

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StorkConfigUtil.addRegistrarTypeIfAbsent("", original, "/custom");
        });

        String expectedMessage = "Parameter type should be provided.";
        String actualMessage = exception.getMessage();

        assertThat(actualMessage).contains(expectedMessage);
    }

    @Test
    public void shouldGetDefaultHostAndPort() {
        //Given empty values for host and port
        Map<String, String> params = new HashMap<>();

        //when getting host and port
        String host = StorkConfigUtil.getOrDefaultHost(params, config);
        int port = StorkConfigUtil.getOrDefaultPort(params, config);

        //should return the first IPv4 address and the quarkus port
        assertThat("localhost").isNotEqualTo(host);
        assertThat(9090).isEqualTo(port);
    }

    @Test
    public void shouldGetCustomHostAndPort() {
        Map<String, String> params = new HashMap<>();
        params.put("ip-address", "145.145.145.145");
        params.put("port", "9999");

        String host = StorkConfigUtil.getOrDefaultHost(params, config);
        int port = StorkConfigUtil.getOrDefaultPort(params, config);

        assertThat("145.145.145.145").isEqualTo(host);
        assertThat(9999).isEqualTo(port);
    }

    @Test
    void shouldReturnValidInetAddress() {
        InetAddress address = StorkConfigUtil.detectAddress();
        assertThat(address).isNotNull();
    }

}

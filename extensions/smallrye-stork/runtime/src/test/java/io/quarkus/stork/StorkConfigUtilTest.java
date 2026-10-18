package io.quarkus.stork;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.stork.api.config.ServiceConfig;

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
    public void shouldBuildRegistrarOnlyConfigurationWithHealthCheck() {
        String healthCheckPath = "/q/health";
        String registrarType = "consul";

        ServiceConfiguration config = StorkConfigUtil.buildRegistrarOnlyConfiguration(registrarType, healthCheckPath);

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
    public void shouldBuildRegistrarOnlyConfigurationWithoutHealthCheck() {
        String healthCheckPath = "";
        String registrarType = "consul";

        ServiceConfiguration config = StorkConfigUtil.buildRegistrarOnlyConfiguration(registrarType, healthCheckPath);

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
            StorkConfigUtil.buildRegistrarOnlyConfiguration(registrarType, healthCheckPath);
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
    public void shouldPreserveDiscoveryAndLoadBalancerWhenAddingRegistrarType() {
        StorkServiceDiscoveryConfiguration discovery = buildDiscoveryConfig("consul", Map.of("consul-host", "myhost"));
        StorkLoadBalancerConfiguration lb = buildLoadBalancerConfig("round-robin", Map.of());
        ServiceConfiguration original = buildServiceConfig(discovery, lb, null);

        ServiceConfiguration updated = StorkConfigUtil.addRegistrarTypeIfAbsent("consul", original, "/health");

        assertThat(updated.serviceRegistrar()).isPresent();
        assertThat(updated.serviceRegistrar().get().type()).hasValue("consul");
        assertThat(updated.serviceRegistrar().get().parameters()).containsEntry("health-check-url", "/health");

        assertThat(updated.serviceDiscovery()).isPresent();
        assertThat(updated.serviceDiscovery().get().type()).isEqualTo("consul");
        assertThat(updated.serviceDiscovery().get().params()).containsEntry("consul-host", "myhost");

        assertThat(updated.loadBalancer()).isNotNull();
        assertThat(updated.loadBalancer().type()).isEqualTo("round-robin");
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

    @Test
    public void shouldNotLeakRegistrarConfigBetweenServices() {
        StorkConfiguration storkConfig = buildStorkConfiguration(
                Map.entry("backend-1", buildServiceConfig(null, null, buildRegistrarConfig("consul", Map.of()))),
                Map.entry("backend-2", buildServiceConfig(buildDiscoveryConfig("consul", Map.of()),
                        buildLoadBalancerConfig("round-robin", Map.of()), null)));

        List<ServiceConfig> result = StorkConfigUtil.toStorkServiceConfig(storkConfig);

        assertThat(result).hasSize(2);

        ServiceConfig backend1 = findByName(result, "backend-1");
        assertThat(backend1.serviceRegistrar()).isNotNull();
        assertThat(backend1.serviceRegistrar().type()).isEqualTo("consul");
        assertThat(backend1.serviceDiscovery()).isNull();

        ServiceConfig backend2 = findByName(result, "backend-2");
        assertThat(backend2.serviceDiscovery()).isNotNull();
        assertThat(backend2.serviceDiscovery().type()).isEqualTo("consul");
        assertThat(backend2.serviceRegistrar()).isNull();
    }

    @Test
    public void shouldNotLeakLoadBalancerConfigBetweenServices() {
        StorkConfiguration storkConfig = buildStorkConfiguration(
                Map.entry("svc-a", buildServiceConfig(buildDiscoveryConfig("consul", Map.of()),
                        buildLoadBalancerConfig("round-robin", Map.of()),
                        buildRegistrarConfig("consul", Map.of()))),
                Map.entry("svc-b", buildServiceConfig(null, null, buildRegistrarConfig("consul", Map.of()))));

        List<ServiceConfig> result = StorkConfigUtil.toStorkServiceConfig(storkConfig);

        ServiceConfig svcB = findByName(result, "svc-b");
        assertThat(svcB.serviceDiscovery()).isNull();
        assertThat(svcB.loadBalancer()).isNull();
        assertThat(svcB.serviceRegistrar()).isNotNull();
    }

    @Test
    public void shouldNotLeakCustomParametersBetweenServices() {
        StorkConfiguration storkConfig = buildStorkConfiguration(
                Map.entry("svc-a", buildServiceConfig(null, null,
                        buildRegistrarConfig("consul", Map.of("ip-address", "10.0.0.1", "port", "9090")))),
                Map.entry("svc-b", buildServiceConfig(null, null,
                        buildRegistrarConfig("consul", Map.of()))));

        List<ServiceConfig> result = StorkConfigUtil.toStorkServiceConfig(storkConfig);

        ServiceConfig svcA = findByName(result, "svc-a");
        assertThat(svcA.serviceRegistrar().parameters()).containsEntry("ip-address", "10.0.0.1");
        assertThat(svcA.serviceRegistrar().parameters()).containsEntry("port", "9090");

        ServiceConfig svcB = findByName(result, "svc-b");
        assertThat(svcB.serviceRegistrar().parameters()).doesNotContainKey("ip-address");
        assertThat(svcB.serviceRegistrar().parameters()).doesNotContainKey("port");
    }

    @Test
    public void shouldHandleServiceWithDiscoveryAndRegistrarCombined() {
        StorkConfiguration storkConfig = buildStorkConfiguration(
                Map.entry("my-service", buildServiceConfig(
                        buildDiscoveryConfig("consul", Map.of("consul-host", "localhost")),
                        buildLoadBalancerConfig("round-robin", Map.of()),
                        buildRegistrarConfig("consul", Map.of("ip-address", "10.0.0.5")))));

        List<ServiceConfig> result = StorkConfigUtil.toStorkServiceConfig(storkConfig);

        assertThat(result).hasSize(1);
        ServiceConfig svc = result.get(0);
        assertThat(svc.serviceDiscovery()).isNotNull();
        assertThat(svc.serviceDiscovery().type()).isEqualTo("consul");
        assertThat(svc.serviceDiscovery().parameters()).containsEntry("consul-host", "localhost");
        assertThat(svc.loadBalancer()).isNotNull();
        assertThat(svc.loadBalancer().type()).isEqualTo("round-robin");
        assertThat(svc.serviceRegistrar()).isNotNull();
        assertThat(svc.serviceRegistrar().type()).isEqualTo("consul");
        assertThat(svc.serviceRegistrar().parameters()).containsEntry("ip-address", "10.0.0.5");
    }

    @Test
    public void shouldHandleServiceWithDiscoveryOnly() {
        StorkConfiguration storkConfig = buildStorkConfiguration(
                Map.entry("discovery-only", buildServiceConfig(
                        buildDiscoveryConfig("eureka", Map.of()),
                        buildLoadBalancerConfig("round-robin", Map.of()), null)));

        List<ServiceConfig> result = StorkConfigUtil.toStorkServiceConfig(storkConfig);

        assertThat(result).hasSize(1);
        ServiceConfig svc = result.get(0);
        assertThat(svc.serviceDiscovery()).isNotNull();
        assertThat(svc.serviceDiscovery().type()).isEqualTo("eureka");
        assertThat(svc.loadBalancer()).isNotNull();
        assertThat(svc.serviceRegistrar()).isNull();
    }

    @Test
    public void shouldHandleServiceWithRegistrarOnly() {
        StorkConfiguration storkConfig = buildStorkConfiguration(
                Map.entry("registrar-only", buildServiceConfig(null, null,
                        buildRegistrarConfig("consul", Map.of()))));

        List<ServiceConfig> result = StorkConfigUtil.toStorkServiceConfig(storkConfig);

        assertThat(result).hasSize(1);
        ServiceConfig svc = result.get(0);
        assertThat(svc.serviceDiscovery()).isNull();
        assertThat(svc.loadBalancer()).isNull();
        assertThat(svc.serviceRegistrar()).isNotNull();
        assertThat(svc.serviceRegistrar().type()).isEqualTo("consul");
    }

    // --- Helper methods to build test configurations ---

    @SafeVarargs
    private static StorkConfiguration buildStorkConfiguration(Map.Entry<String, ServiceConfiguration>... entries) {
        Map<String, ServiceConfiguration> map = new LinkedHashMap<>();
        for (Map.Entry<String, ServiceConfiguration> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return () -> map;
    }

    private static ServiceConfiguration buildServiceConfig(StorkServiceDiscoveryConfiguration discovery,
            StorkLoadBalancerConfiguration loadBalancer,
            StorkServiceRegistrarConfiguration registrar) {
        return new ServiceConfiguration() {
            @Override
            public Optional<StorkServiceDiscoveryConfiguration> serviceDiscovery() {
                return Optional.ofNullable(discovery);
            }

            @Override
            public StorkLoadBalancerConfiguration loadBalancer() {
                return loadBalancer;
            }

            @Override
            public Optional<StorkServiceRegistrarConfiguration> serviceRegistrar() {
                return Optional.ofNullable(registrar);
            }
        };
    }

    private static StorkServiceDiscoveryConfiguration buildDiscoveryConfig(String type, Map<String, String> params) {
        return new StorkServiceDiscoveryConfiguration() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public Map<String, String> params() {
                return params;
            }
        };
    }

    private static StorkLoadBalancerConfiguration buildLoadBalancerConfig(String type, Map<String, String> parameters) {
        return new StorkLoadBalancerConfiguration() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public Map<String, String> parameters() {
                return parameters;
            }
        };
    }

    private static StorkServiceRegistrarConfiguration buildRegistrarConfig(String type, Map<String, String> parameters) {
        return new StorkServiceRegistrarConfiguration() {
            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public Optional<String> type() {
                return Optional.of(type);
            }

            @Override
            public Map<String, String> parameters() {
                return new HashMap<>(parameters);
            }
        };
    }

    private static ServiceConfig findByName(List<ServiceConfig> configs, String name) {
        return configs.stream()
                .filter(c -> c.serviceName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Service '" + name + "' not found"));
    }

}

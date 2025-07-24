package io.quarkus.stork;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
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

        //Registers config for ConfigProvider.getConfig() to return
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void shouldBuildDefaultRegistrarConfigurationWithHealthCheck() {
        String healthCheckPath = "/q/health";
        String registrarType = "consul";

        ServiceConfiguration config = StorkConfigUtil.buildDefaultRegistrarConfiguration(registrarType, healthCheckPath);

        assertTrue(config.serviceRegistrar().isPresent());
        var registrar = config.serviceRegistrar().get();

        assertTrue(registrar.type().isPresent());
        assertEquals(registrarType, registrar.type().get());
        assertTrue(registrar.parameters().containsKey("health-check-url"));

        String hcUrl = registrar.parameters().get("health-check-url");
        assertTrue(hcUrl.contains("/q/health"));
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

        assertTrue(updated.serviceRegistrar().isPresent());
        var registrar = updated.serviceRegistrar().get();

        assertTrue(registrar.type().isPresent());
        assertEquals("consul", registrar.type().get());
        assertEquals("/custom", registrar.parameters().get("health-check-url"));
    }

    @Test
    public void shouldGetDefaultHostAndPort() {
        //Given empty values for host and port
        Map<String, String> params = new HashMap<>();

        //when getting host and port
        String host = StorkConfigUtil.getOrDefaultHost(params, config);
        int port = StorkConfigUtil.getOrDefaultPort(params, config);

        //should return the first IPv4 address and the quarkus port
        assertNotEquals("localhost", host);
        assertEquals(9090, port);
    }

    @Test
    public void shouldGetCustomHostAndPort() {
        Map<String, String> params = new HashMap<>();
        params.put("ip-address", "145.145.145.145");
        params.put("port", "9999");

        String host = StorkConfigUtil.getOrDefaultHost(params, config);
        int port = StorkConfigUtil.getOrDefaultPort(params, config);

        assertEquals("145.145.145.145", host);
        assertEquals(9999, port);
    }

    @Test
    void shouldReturnValidInetAddress() {
        InetAddress address = StorkConfigUtil.detectAddress();
        assertNotNull(address, "Address should not be null");
        System.out.println("Detected IP: " + address.getHostAddress());
    }

    @Test
    void shouldReturnFirstValidInet4Address() throws Exception {
        InetAddress expected = InetAddress.getByName("192.168.0.10");

        NetworkInterfaceWrapper mockIface = new NetworkInterfaceWrapper() {
            @Override
            public boolean isUp() {
                return true;
            }

            @Override
            public boolean isLoopback() {
                return false;
            }

            @Override
            public Enumeration<InetAddress> getInetAddresses() {
                return Collections.enumeration(List.of(expected));
            }
        };

        InetAddress result = StorkConfigUtil.findFirstValidAddress(List.of(mockIface));
        assertEquals(expected, result);
    }

}

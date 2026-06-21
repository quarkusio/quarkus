package io.quarkus.spring.boot.properties.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.quarkus.test.QuarkusUnitTest;

class ConfigurationPropertiesTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsServiceProvider(Converter.class, ServerConverter.class)
                    .addAsResource(new StringAsset(
                            """
                                    config.spring.map.one=one
                                    config.spring.map.two=two
                                    config.spring.servers.one=host: localhost, port: 8080
                                    config.spring.required.one=one
                                    """), "application.properties"));

    @Inject
    MapConfig mapConfig;
    @Inject
    MapInterfaceConfig mapInterfaceConfig;

    @Test
    void config() {
        assertFalse(mapConfig.getMap().isEmpty());
        assertEquals("one", mapConfig.getMap().get("one"));
        assertEquals("two", mapConfig.getMap().get("two"));
        assertEquals("localhost", mapConfig.getServers().get("one").getHost());
        assertEquals(8080, mapConfig.getServers().get("one").getPort());
        assertEquals("one", mapConfig.required.get("one"));

        assertFalse(mapInterfaceConfig.map().isEmpty());
        assertEquals("one", mapInterfaceConfig.map().get("one"));
        assertEquals("two", mapInterfaceConfig.map().get("two"));
        assertEquals("localhost", mapInterfaceConfig.servers().get("one").getHost());
        assertEquals(8080, mapInterfaceConfig.servers().get("one").getPort());
        assertEquals("one", mapInterfaceConfig.required().get("one"));
    }

    @ConfigurationProperties("config.spring")
    public static final class MapConfig {
        private Map<String, String> map = new HashMap<>();
        private Map<String, Server> servers = new HashMap<>();

        private Map<String, String> required = new HashMap<>();

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }

        public Map<String, Server> getServers() {
            return servers;
        }

        public void setServers(Map<String, Server> servers) {
            this.servers = servers;
        }

        public void setRequired(Map<String, String> required) {
            this.required = required;
        }
    }

    @ConfigurationProperties("config.spring")
    public interface MapInterfaceConfig {
        Map<String, String> map();

        Map<String, Server> servers();

        Map<String, String> required();
    }

    public static final class Server {
        private String host;
        private int port;

        public String getHost() {
            return host;
        }

        public Server setHost(String host) {
            this.host = host;
            return this;
        }

        public int getPort() {
            return port;
        }

        public Server setPort(int port) {
            this.port = port;
            return this;
        }
    }

    public static final class ServerConverter implements Converter<Server> {
        @Override
        public Server convert(String value) throws IllegalArgumentException, NullPointerException {
            Map<String, String> map = new HashMap<>();
            String[] values = value.split(",");
            for (String element : values) {
                String[] entry = element.trim().split(":");
                map.put(entry[0].trim(), entry[1].trim());
            }

            return new Server().setHost(map.get("host")).setPort(Integer.parseInt(map.get("port")));
        }
    }
}

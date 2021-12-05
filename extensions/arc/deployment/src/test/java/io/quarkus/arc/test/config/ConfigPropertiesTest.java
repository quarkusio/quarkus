package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

public class ConfigPropertiesTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "smallrye.config.mapping.validate-unknown=false\n" +
                                    "server.host=localhost\n" +
                                    "server.port=8080\n" +
                                    "server.reasons.200=OK Server\n" +
                                    "server.reasons.201=Created Server\n" +
                                    "cloud.host=cloud\n" +
                                    "cloud.port=9090\n" +
                                    "cloud.reasons.200=OK Cloud\n" +
                                    "cloud.reasons.201=Created Cloud\n" +
                                    "host=empty\n" +
                                    "port=0\n" +
                                    "reasons.200=OK\n" +
                                    "reasons.201=Created\n"),
                            "application.properties"));

    @Inject
    Server server;

    @Test
    void configMapping() {
        assertNotNull(server);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Inject
    Client client;

    @Test
    void discoveredMapping() {
        assertNotNull(client);
        assertEquals("client", client.host());
        assertEquals(80, client.port());
    }

    @Inject
    @ConfigMapping(prefix = "cloud")
    Server cloud;

    @Test
    void overridePrefix() {
        assertNotNull(cloud);
        assertEquals("cloud", cloud.host());
        assertEquals(9090, cloud.port());
    }

    @Inject
    @ConfigProperties
    ServerConfigProperties configProperties;
    @Inject
    @ConfigProperties(prefix = "cloud")
    ServerConfigProperties configPropertiesCloud;
    @Inject
    @ConfigProperties(prefix = "")
    ServerConfigProperties configPropertiesEmpty;

    @Test
    void configProperties() {
        assertNotNull(configProperties);
        assertEquals("localhost", configProperties.host);
        assertEquals(8080, configProperties.port);
        assertEquals(2, configProperties.reasons.size());
        assertEquals("OK Server", configProperties.reasons.get(200));
        assertEquals("Created Server", configProperties.reasons.get(201));

        assertNotNull(configPropertiesCloud);
        assertEquals("cloud", configPropertiesCloud.host);
        assertEquals(9090, configPropertiesCloud.port);
        assertEquals(2, configPropertiesCloud.reasons.size());
        assertEquals("OK Cloud", configPropertiesCloud.reasons.get(200));
        assertEquals("Created Cloud", configPropertiesCloud.reasons.get(201));

        assertNotNull(configPropertiesEmpty);
        assertEquals("empty", configPropertiesEmpty.host);
        assertEquals(0, configPropertiesEmpty.port);
        assertEquals(2, configPropertiesEmpty.reasons.size());
        assertEquals("OK", configPropertiesEmpty.reasons.get(200));
        assertEquals("Created", configPropertiesEmpty.reasons.get(201));
    }

    @Test
    void select() {
        Server server = CDI.current().select(Server.class).get();
        assertNotNull(server);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());

        ServerConfigProperties serverConfigProperties = CDI.current()
                .select(ServerConfigProperties.class, ConfigProperties.Literal.NO_PREFIX).get();
        assertNotNull(serverConfigProperties);
        assertEquals("localhost", serverConfigProperties.host);
        assertEquals(8080, serverConfigProperties.port);

        ServerConfigProperties cloudConfigProperties = CDI.current()
                .select(ServerConfigProperties.class, ConfigProperties.Literal.of("cloud")).get();
        assertNotNull(cloudConfigProperties);
        assertEquals("cloud", cloudConfigProperties.host);
        assertEquals(9090, cloudConfigProperties.port);
    }

    @Inject
    ConfigMappingBean configMappingBean;

    @Test
    void overridePrefixBean() {
        Server cloud = configMappingBean.getCloud();
        assertEquals("cloud", cloud.host());
        assertEquals(9090, cloud.port());

        Server client = configMappingBean.getClient();
        assertEquals("client", client.host());
        assertEquals(80, client.port());
    }

    @ConfigMapping(prefix = "server")
    public interface Server {
        String host();

        int port();
    }

    @ConfigMapping(prefix = "client")
    public interface Client {
        @WithDefault("client")
        String host();

        @WithDefault("80")
        int port();
    }

    @ConfigProperties(prefix = "server")
    public static class ServerConfigProperties {
        public String host;
        public int port;
        public Map<Integer, String> reasons;
    }

    @Dependent
    static class ConfigMappingBean {
        private final Server cloud;
        private Server client;

        @Inject
        public ConfigMappingBean(@ConfigMapping(prefix = "cloud") Server cloud) {
            this.cloud = cloud;
        }

        public Server getCloud() {
            return cloud;
        }

        public Server getClient() {
            return client;
        }

        @Inject
        public void setClient(@ConfigMapping(prefix = "client") final Server client) {
            this.client = client;
        }
    }
}

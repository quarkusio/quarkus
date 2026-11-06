package io.quarkus.spring.boot.properties.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMapping.NamingStrategy;

class ConfigurationPropertiesNamingStrategyTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            """
                                    verbatim.appName=my-app
                                    verbatim.hostName=localhost
                                    verbatim.server.baseUrl=http://localhost
                                    verbatim.pool.max_size=20
                                    verbatim.nodes[0].nodeName=main
                                    verbatim.groups.search.groupDescription=Search tools
                                    verbatim.endpoint.base-url=http://endpoint
                                    """), "application.properties"));

    @Inject
    VerbatimProperties verbatim;

    @Test
    void namingStrategy() {
        assertEquals("my-app", verbatim.appName);
        assertEquals("localhost", verbatim.host);
        assertEquals("http://localhost", verbatim.server.baseUrl);
        assertEquals(20, verbatim.pool.maxSize);
        assertEquals("main", verbatim.nodes.get(0).nodeName);
        assertEquals("Search tools", verbatim.groups.get("search").groupDescription);
        assertEquals("http://endpoint", verbatim.endpoint.baseUrl);
    }

    @ConfigurationProperties(prefix = "verbatim")
    @ConfigMapping(namingStrategy = NamingStrategy.VERBATIM)
    public static class VerbatimProperties {
        public String appName;
        // an explicit name is used as is
        @Name("hostName")
        public String host;
        // nested classes inherit the naming strategy of the enclosing class
        public Server server;
        public Pool pool;
        public List<Node> nodes;
        public Map<String, Group> groups;
        // a class not enclosed by the annotated class falls back to kebab-case
        public Endpoint endpoint;

        public static class Server {
            public String baseUrl;
        }

        // a nested class may declare its own naming strategy
        @ConfigMapping(namingStrategy = NamingStrategy.SNAKE_CASE)
        public static class Pool {
            public int maxSize;
        }

        public static class Node {
            public String nodeName;
        }

        public static class Group {
            public String groupDescription;
        }
    }

    public static class Endpoint {
        public String baseUrl;
    }
}

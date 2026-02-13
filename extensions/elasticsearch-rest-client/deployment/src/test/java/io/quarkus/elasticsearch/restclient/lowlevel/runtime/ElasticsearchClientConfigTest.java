package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Identifier;

public class ElasticsearchClientConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClasses(TestConfigurator.class)
                            .addAsResource(new StringAsset("quarkus.elasticsearch.hosts=elasticsearch:9200"),
                                    "application.properties"));

    @Inject
    ElasticsearchClientsRuntimeConfig config;

    @Inject
    RestClient client;

    @Inject
    @Identifier("second-client")
    RestClient secondClient;

    @Test
    public void testRestClientBuilderHelperWithElasticsearchClientConfig() {
        assertNotNull(client);
        assertNotNull(secondClient);
        assertTrue(TestConfigurator.invoked);
        assertTrue(SecondTestConfigurator.invoked);
    }

    @ElasticsearchClientConfig
    @ApplicationScoped
    public static class TestConfigurator implements RestClientBuilder.HttpClientConfigCallback {

        private static boolean invoked = false;

        @Override
        public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder builder) {
            invoked = true;
            return builder;
        }
    }

    @ElasticsearchClientConfig("second-client")
    @ApplicationScoped
    public static class SecondTestConfigurator implements RestClientBuilder.HttpClientConfigCallback {

        private static boolean invoked = false;

        @Override
        public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder builder) {
            invoked = true;
            return builder;
        }
    }

}

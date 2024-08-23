package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;
import io.quarkus.test.QuarkusUnitTest;

public class ElasticsearchClientConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClasses(TestConfigurator.class, RestClientBuilderHelper.class)
                            .addAsResource(new StringAsset("quarkus.elasticsearch.hosts=elasticsearch:9200"),
                                    "application.properties"));

    @Inject
    ElasticsearchConfig config;

    @Test
    public void testRestClientBuilderHelperWithElasticsearchClientConfig() {
        RestClientBuilderHelper.createRestClientBuilder(config).build();
        assertTrue(TestConfigurator.invoked);
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

}

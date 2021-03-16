package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
                    () -> ShrinkWrap.create(JavaArchive.class).addClass(TestConfigurator.class)
                            .addAsResource(new StringAsset("quarkus.elasticsearch.hosts=elasticsearch:9200"),
                                    "application.properties"));

    @Inject
    ElasticsearchConfig config;
    @Inject
    @ElasticsearchClientConfig
    TestConfigurator testConfigurator;

    @Test
    public void testRestClientBuilderHelperWithElasticsearchClientConfig() {
        RestClientBuilderHelper.createRestClientBuilder(config).build();
        assertTrue(testConfigurator.isInvoked());
    }

    @ElasticsearchClientConfig
    @ApplicationScoped
    public static class TestConfigurator implements RestClientBuilder.HttpClientConfigCallback {
        private boolean invoked = false;

        @Override
        public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder builder) {
            invoked = true;
            return builder;
        }

        public boolean isInvoked() {
            return invoked;
        }
    }

}

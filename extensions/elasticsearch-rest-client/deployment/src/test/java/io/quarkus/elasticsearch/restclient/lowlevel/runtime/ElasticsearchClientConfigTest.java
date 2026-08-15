package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;
import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfigConfigurer;
import io.quarkus.test.QuarkusExtensionTest;

public class ElasticsearchClientConfigTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
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
    public static class TestConfigurator implements ElasticsearchClientConfigConfigurer {

        private static boolean invoked = false;

        @Override
        public void accept(HttpAsyncClientBuilder builder) {
            invoked = true;
        }
    }

}

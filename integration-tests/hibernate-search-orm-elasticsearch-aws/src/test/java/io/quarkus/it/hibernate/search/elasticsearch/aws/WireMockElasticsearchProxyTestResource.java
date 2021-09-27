package io.quarkus.it.hibernate.search.elasticsearch.aws;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class WireMockElasticsearchProxyTestResource implements QuarkusTestResourceLifecycleManager {

    WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();

        // See surefire/failsafe configuration in pom.xml
        String elasticsearchProtocol = System.getProperty("elasticsearch.protocol");
        String elasticsearchHosts = System.getProperty("elasticsearch.hosts");
        String elasticsearchFirstHost = elasticsearchHosts.split(",")[0];

        wireMockServer.stubFor(any(anyUrl())
                .willReturn(aResponse().proxiedFrom(elasticsearchProtocol + "://" + elasticsearchFirstHost)));

        return Map.of(
                "quarkus.hibernate-search-orm.elasticsearch.hosts", "localhost:" + wireMockServer.port());
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(wireMockServer,
                new TestInjector.AnnotatedAndMatchesType(InjectWireMock.class, WireMockServer.class));
    }

    @Override
    public synchronized void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }
}

package io.quarkus.it.hibernate.search.elasticsearch.aws;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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
    public void inject(Object testInstance) {
        Class<?> c = testInstance.getClass();
        Class<? extends Annotation> annotation = InjectWireMock.class;
        Class<?> injectedClass = WireMockServer.class;
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getAnnotation(annotation) != null) {
                    if (!injectedClass.isAssignableFrom(f.getType())) {
                        throw new RuntimeException(annotation + " can only be used on fields of type " + injectedClass);
                    }
                    f.setAccessible(true);
                    try {
                        f.set(testInstance, wireMockServer);
                        return;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    @Override
    public synchronized void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }
}

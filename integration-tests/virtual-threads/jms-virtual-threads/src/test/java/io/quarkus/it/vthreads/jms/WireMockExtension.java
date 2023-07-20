package io.quarkus.it.vthreads.jms;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class WireMockExtension implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();

        wireMockServer.stubFor(post(urlEqualTo("/price/alert"))
                .willReturn(aResponse().withBody("ok")));
        wireMockServer.stubFor(post(urlEqualTo("/price/alert-message"))
                .willReturn(aResponse().withBody("ok")));

        return Map.of("price-alert/mp-rest/url", wireMockServer.baseUrl());
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(wireMockServer, f -> f.getType().isAssignableFrom(WireMockServer.class));
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }
}

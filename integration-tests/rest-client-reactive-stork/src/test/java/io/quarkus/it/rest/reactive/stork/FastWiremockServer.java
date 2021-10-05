package io.quarkus.it.rest.reactive.stork;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

public class FastWiremockServer extends WiremockBase {

    static final String FAST_RESPONSE = "hello, I'm a fast server";

    @Override
    int port() {
        return 8766;
    }

    @Override
    protected Map<String, String> initWireMock(WireMockServer server) {
        server.stubFor(WireMock.get("/hello")
                .willReturn(aResponse().withBody(FAST_RESPONSE).withStatus(200)));
        return Map.of("stork.hello-service.service-discovery.1", "localhost:8766");
    }
}

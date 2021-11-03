package io.quarkus.it.rest.reactive.stork;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

public class SlowWiremockServer extends WiremockBase {

    static final String SLOW_RESPONSE = "hello, I'm a slow server";

    @Override
    int port() {
        return 8767;
    }

    @Override
    protected Map<String, String> initWireMock(WireMockServer server) {
        server.stubFor(WireMock.get("/hello")
                .willReturn(aResponse().withFixedDelay(1000)
                        .withBody(SLOW_RESPONSE).withStatus(200)));
        return Map.of("stork.hello-service.service-discovery.2", "localhost:8767");
    }
}

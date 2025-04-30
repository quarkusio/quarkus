package io.quarkus.it.rest.reactive.stork;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

public class SlowWiremockServer extends WiremockBase {

    static final String SLOW_RESPONSE = "hello, I'm a slow server";

    @Override
    int httpPort() {
        return 8767;
    }

    @Override
    int httpsPort() {
        return 8444;
    }

    @Override
    protected Map<String, String> initWireMock(WireMockServer server) {
        server.stubFor(WireMock.get("/hello")
                .willReturn(aResponse().withFixedDelay(1000)
                        .withBody(SLOW_RESPONSE).withStatus(200)));
        server.stubFor(WireMock.get(urlPathTemplate("/hello/v2/{name}"))
                .willReturn(aResponse().withFixedDelay(1000).withBody(SLOW_RESPONSE).withStatus(200)));
        return Map.of("slow-service", "localhost:8444");
    }
}

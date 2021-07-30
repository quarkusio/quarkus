package io.quarkus.it.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class WireMockZipkin implements QuarkusTestResourceLifecycleManager {

    private static WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();

        stubFor(post(anyUrl()).withRequestBody(equalToJson(
                "[{\"name\": \"get:io.quarkus.it.rest.client.server.echoservice.echo\",\"tags\": {\"component\": \"jaxrs\",\"http.method\": \"GET\",\"http.status_code\": \"200\",\"sampler.param\": \"true\",\"sampler.type\": \"const\"}}]",
                true, true)).willReturn(aResponse().withStatus(200)));

        return Collections.singletonMap("quarkus.jaeger.endpoint", wireMockServer.baseUrl() + "/zipkin");
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }

    public static WireMockServer getWireMockServer() {
        return wireMockServer;
    }

}

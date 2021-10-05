package io.quarkus.it.rest.reactive.stork;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public abstract class WiremockBase implements QuarkusTestResourceLifecycleManager {
    private WireMockServer server;

    abstract Map<String, String> initWireMock(WireMockServer server);

    abstract int port();

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(options().port(port()));

        var result = initWireMock(server);
        server.start();

        return result;
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}

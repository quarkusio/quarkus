package io.quarkus.websockets;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.Extension;

import io.vertx.core.http.HttpHeaders;

public class BearerTokenClientEndpointConfigurator implements ClientEndpointConfig {

    final String token;

    public BearerTokenClientEndpointConfigurator(String token) {
        this.token = token;
    }

    @Override
    public List<String> getPreferredSubprotocols() {
        return Collections.emptyList();
    }

    @Override
    public List<Extension> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    public Configurator getConfigurator() {
        return new Configurator() {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                headers.put(HttpHeaders.AUTHORIZATION.toString(), Collections.singletonList("Bearer " + token));
            }
        };
    }

    @Override
    public List<Class<? extends Encoder>> getEncoders() {
        return Collections.emptyList();
    }

    @Override
    public List<Class<? extends Decoder>> getDecoders() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return Collections.emptyMap();
    }

    @Override
    public SSLContext getSSLContext() {
        return null;
    }
}

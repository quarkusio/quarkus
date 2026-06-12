package io.quarkus.http3.runtime;

import java.util.Set;

import jakarta.enterprise.context.Dependent;

import io.quarkus.vertx.http.HttpServerConfigCustomizer;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ServerSSLOptions;

@Dependent
public class Http3Customizer implements HttpServerConfigCustomizer {

    @Override
    public void customizeHttpsServer(HttpServerConfig config, ServerSSLOptions sslOptions) {
        Set<HttpVersion> versions = config.getVersions();
        versions.add(HttpVersion.HTTP_3);
        config.setVersions(versions);
    }

}

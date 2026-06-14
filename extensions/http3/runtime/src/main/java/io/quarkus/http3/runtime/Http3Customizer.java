package io.quarkus.http3.runtime;

import java.util.Set;

import jakarta.enterprise.context.Dependent;

import org.jboss.logging.Logger;

import io.quarkus.vertx.http.HttpServerConfigCustomizer;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ServerSSLOptions;

@Dependent
public class Http3Customizer implements HttpServerConfigCustomizer {

    private static final Logger LOG = Logger.getLogger("io.quarkus.http3");

    static volatile boolean httpsConfigured;
    static volatile CertOrigin certOrigin = CertOrigin.CONFIGURED;

    @Override
    public void customizeHttpsServer(HttpServerConfig config, ServerSSLOptions sslOptions) {
        httpsConfigured = true;
        Set<HttpVersion> versions = config.getVersions();
        versions.add(HttpVersion.HTTP_3);
        config.setVersions(versions);

        logHttp3Status(config.getTcpHost(), config.getQuicPort());
    }

    private void logHttp3Status(String host, int port) {
        StringBuilder msg = new StringBuilder("HTTP/3 (QUIC/UDP) available on https://");
        msg.append(host != null ? host : "0.0.0.0");
        if (port > 0) {
            msg.append(':').append(port);
        }

        switch (certOrigin) {
            case DEV_CA -> msg.append(" (auto-generated certificate, signed by Quarkus Dev CA)");
            case SELF_SIGNED -> msg.append(" (auto-generated self-signed certificate)");
            case CONFIGURED -> {
                /* nothing to add */ }
        }

        LOG.info(msg.toString());
    }

}

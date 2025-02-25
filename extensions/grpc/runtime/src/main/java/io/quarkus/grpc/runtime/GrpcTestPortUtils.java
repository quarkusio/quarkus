package io.quarkus.grpc.runtime;

import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.vertx.core.http.ClientAuth;

public final class GrpcTestPortUtils {
    private GrpcTestPortUtils() {
    }

    public static int testPort(GrpcServerConfiguration serverConfiguration) {
        if (serverConfiguration.useSeparateServer()) {
            if (serverConfiguration.testPort() == 0) {
                return testPort("grpc.server");
            }
            return serverConfiguration.testPort();
        }
        if (isHttpsConfigured(serverConfiguration.ssl()) || !serverConfiguration.isPlainTextEnabled()) {
            int httpsTestPort = port("quarkus.http.test-ssl-port");
            if (httpsTestPort == 0) {
                return testPort("https");
            }
            return httpsTestPort;
        }
        return testPort("http");
    }

    private static boolean isHttpsConfigured(GrpcServerConfiguration.SslServerConfig ssl) {
        return ssl.certificate().isPresent() || ssl.key().isPresent() || ssl.keyStore().isPresent()
                || ssl.keyStoreType().isPresent() || ssl.keyStorePassword().isPresent() || ssl.trustStore().isPresent()
                || ssl.trustStoreType().isPresent() || ssl.cipherSuites().isPresent() || ssl.clientAuth() != ClientAuth.NONE
                || !isDefaultProtocols(ssl.protocols());
    }

    private static boolean isDefaultProtocols(Set<String> protocols) {
        return protocols.size() == 2 && protocols.contains("TLSv1.3") && protocols.contains("TLSv1.2");
    }

    private static int testPort(String subProperty) {
        return port("quarkus." + subProperty + ".test-port");
    }

    private static int port(String property) {
        return ConfigProvider.getConfig().getValue(property, Integer.class);
    }
}

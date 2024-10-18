package io.quarkus.runtime.configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Services {
    private static final Map<String, Service> services = new ConcurrentHashMap<>();

    private Services() {
        throw new UnsupportedOperationException();
    }

    public static Optional<Service> get(String name) {
        return Optional.ofNullable(services.get(name));
    }

    public static void register(final String service, final String protocol, final String host, final int port) {
        services.put(service, new Service(protocol, host, port));
    }

    public static class Service {
        private final String protocol;
        private final String host;
        private final int port;

        public Service(final String protocol, final String host, final int port) {
            this.protocol = protocol;
            this.host = host;
            this.port = port;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public URI toUri() {
            return URI.create(protocol + "://" + host + ":" + port);
        }

        public URL toUrl() {
            try {
                return toUri().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException();
            }
        }
    }
}
package io.quarkus.vertx.http;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.quarkus.stork.QuarkusServiceInstance;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryType("vertx-http")
public class VertxHttpServiceDiscoveryProvider implements ServiceDiscoveryProvider<VertxHttpServiceConfig> {

    @Override
    public ServiceDiscovery createServiceDiscovery(
            VertxHttpServiceConfig config,
            String serviceName,
            ServiceConfig serviceConfig,
            StorkInfrastructure storkInfrastructure) {

        return new ServiceDiscovery() {
            @Override
            public Uni<List<ServiceInstance>> getServiceInstances() {
                return Uni.createFrom().item(() -> instances(config, serviceName));
            }
        };
    }

    private static List<ServiceInstance> instances(VertxHttpServiceConfig config, String serviceName) {
        if (config.getDomainSocket() != null) {
            return List.of(new QuarkusServiceInstance() {
                @Override
                public String getScheme() {
                    return "http";
                }

                @Override
                public long getId() {
                    return 0;
                }

                @Override
                public String getHost() {
                    return host(config.getHttpServer());
                }

                @Override
                public int getPort() {
                    return -1;
                }

                @Override
                public Optional<String> getPath() {
                    return Optional.empty();
                }

                @Override
                public boolean isSecure() {
                    return false;
                }

                @Override
                public Optional<String> getDomainSocket() {
                    return Optional.of(config.getDomainSocket());
                }
            });
        }

        HttpServer httpServer = config.getHttpServer();

        // Prefer the secure listener. Turning on TLS leaves the plain HTTP port open by default
        // (quarkus.http.insecure-requests defaults to `enabled`), so both are normally up and a caller asking for
        // "the network address" wants the one it would hand out.
        int securePort = httpServer.getSecurePort();
        if (securePort > 0) {
            return List.of(new QuarkusServiceInstance() {
                @Override
                public String getScheme() {
                    return "https";
                }

                @Override
                public long getId() {
                    return 0;
                }

                @Override
                public String getHost() {
                    return host(config.getHttpServer());
                }

                @Override
                public int getPort() {
                    return securePort;
                }

                @Override
                public Optional<String> getPath() {
                    return Optional.empty();
                }

                @Override
                public boolean isSecure() {
                    return true;
                }
            });
        }

        int port = httpServer.getPort();
        if (port > 0) {
            return List.of(new QuarkusServiceInstance() {
                @Override
                public String getScheme() {
                    return "http";
                }

                @Override
                public long getId() {
                    return 0;
                }

                @Override
                public String getHost() {
                    return host(config.getHttpServer());
                }

                @Override
                public int getPort() {
                    return port;
                }

                @Override
                public Optional<String> getPath() {
                    return Optional.empty();
                }

                @Override
                public boolean isSecure() {
                    return false;
                }
            });
        }

        // Nothing is listening on TCP.
        return Collections.emptyList();
    }

    private static String host(final HttpServer httpServer) {
        try {
            URI localBaseUri = httpServer.getLocalBaseUri();
            if (localBaseUri != null && localBaseUri.getHost() != null) {
                return localBaseUri.getHost();
            }
        } catch (RuntimeException ignored) {
            // If not binded yet by the ValueRegistry
        }
        return "localhost";
    }
}

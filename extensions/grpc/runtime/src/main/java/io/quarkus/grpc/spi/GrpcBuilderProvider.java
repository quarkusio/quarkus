package io.quarkus.grpc.spi;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.vertx.core.Vertx;

/**
 * Allow for additional types of gRPC server and channels to be used / built. This is an experimental SPI, subject to
 * change.
 */
public interface GrpcBuilderProvider<S extends ServerBuilder<S>> {

    Logger log = LoggerFactory.getLogger(GrpcBuilderProvider.class);

    /**
     * Find gRPC server builder provider.
     *
     * @param configuration
     *        the gRPC server configuration
     *
     * @return provider instance or null if none is provided
     */
    @SuppressWarnings("rawtypes")
    static GrpcBuilderProvider findServerBuilderProvider(GrpcServerConfiguration configuration) {
        GrpcBuilderProvider provider = null;
        ServiceLoader<GrpcBuilderProvider> providers = ServiceLoader.load(GrpcBuilderProvider.class);
        for (GrpcBuilderProvider p : providers) {
            if (p.providesServer(configuration)) {
                if (provider != null) {
                    throw new IllegalArgumentException("Too many GrpcBuilderProviders enabled: " + providers);
                }
                log.info("Found server GrpcBuilderProvider: {}", p);
                provider = p;
            }
        }
        return provider;
    }

    /**
     * Find gRPC client builder provider.
     *
     * @param configuration
     *        the gRPC client configuration
     *
     * @return provider instance or null if none is provided
     */
    @SuppressWarnings("rawtypes")
    static GrpcBuilderProvider findChannelBuilderProvider(GrpcClientConfiguration configuration) {
        GrpcBuilderProvider provider = null;
        ServiceLoader<GrpcBuilderProvider> providers = ServiceLoader.load(GrpcBuilderProvider.class);
        for (GrpcBuilderProvider p : providers) {
            if (p.providesChannel(configuration)) {
                if (provider != null) {
                    throw new IllegalArgumentException("Too many GrpcBuilderProviders enabled: " + providers);
                }
                log.info("Found channel GrpcBuilderProvider: {}", p);
                provider = p;
            }
        }
        return provider;
    }

    /**
     * Does this builder provider provide a new gRPC server instance.
     *
     * @param configuration
     *        the gRPC server configuration
     *
     * @return true if yes, false if no
     */
    boolean providesServer(GrpcServerConfiguration configuration);

    /**
     * Create initial server builder.
     *
     * @param vertx
     *        the Vertx instance
     * @param configuration
     *        the gRPC server configuration
     * @param launchMode
     *        current launch mode
     *
     * @return new ServerBuilder instance
     */
    ServerBuilder<S> createServerBuilder(Vertx vertx, GrpcServerConfiguration configuration, LaunchMode launchMode);

    /**
     * Start gRPC server.
     *
     * @param server
     *        the server instance to start
     *
     * @throws Exception
     *         for any exception while starting the server
     */
    void startServer(Server server) throws Exception;

    /**
     * Post startup.
     *
     * @param server
     *        the started server
     * @param shutdown
     *        the shutdown hook
     */
    void postStartup(Server server, ShutdownContext shutdown);

    /**
     * Handle dev mode reload.
     *
     * @param servicesWithInterceptors
     *        the services
     * @param methods
     *        the methods
     * @param globalInterceptors
     *        the global interceptors
     * @param shutdown
     *        the shutdown hook
     */
    void devModeReload(List<ServerServiceDefinition> servicesWithInterceptors,
            Map<String, ServerMethodDefinition<?, ?>> methods, List<ServerInterceptor> globalInterceptors,
            ShutdownContext shutdown);

    /**
     * Does a server instance already exist.
     *
     * @return true if a server instance already exists, false otherwise
     */
    boolean serverAlreadyExists();

    /**
     * Provide server info.
     *
     * @param host
     *        server host
     * @param port
     *        server port
     * @param configuration
     *        full server configuration
     *
     * @return simple server info
     */
    String serverInfo(String host, int port, GrpcServerConfiguration configuration);

    /**
     * Does this builder provider provide a new gRPC channel instance.
     *
     * @param configuration
     *        the gRPC client configuration
     *
     * @return true if yes, false if no
     */
    boolean providesChannel(GrpcClientConfiguration configuration);

    /**
     * Get resolver.
     *
     * @return the resolver
     */
    String resolver();

    /**
     * Adjust host, if needed. By default, no adjustment is made.
     *
     * @param host
     *        the host
     *
     * @return adjusted host, if needed
     */
    default String adjustHost(String host) {
        return host;
    }

    /**
     * Create initial channel builder.
     *
     * @param configuration
     *        the gRPC client configuration
     * @param target
     *        the channel target
     *
     * @return new ChannelBuilder
     */
    ManagedChannelBuilder<?> createChannelBuilder(GrpcClientConfiguration configuration, String target);

    /**
     * Provide channel info.
     *
     * @param configuration
     *        client configuration
     *
     * @return simple channel info
     */
    String channelInfo(GrpcClientConfiguration configuration);
}

package io.quarkus.grpc.xds;

import static io.quarkus.grpc.runtime.config.GrpcClientConfiguration.XDS;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.InsecureServerCredentials;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCredentials;
import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.xds.XdsChannelCredentials;
import io.grpc.xds.XdsServerBuilder;
import io.grpc.xds.XdsServerCredentials;
import io.quarkus.grpc.runtime.config.ClientXds;
import io.quarkus.grpc.runtime.config.Enabled;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.config.Xds;
import io.quarkus.grpc.runtime.devmode.DevModeInterceptor;
import io.quarkus.grpc.runtime.devmode.GrpcHotReplacementInterceptor;
import io.quarkus.grpc.spi.GrpcBuilderProvider;
import io.quarkus.grpc.xds.devmode.XdsServerReloader;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;

public class XdsGrpcServerBuilderProvider implements GrpcBuilderProvider<XdsServerBuilder> {
    @Override
    public boolean providesServer(GrpcServerConfiguration configuration) {
        return Enabled.isEnabled(configuration.xds);
    }

    @Override
    public ServerBuilder<XdsServerBuilder> createServerBuilder(Vertx vertx, GrpcServerConfiguration configuration,
            LaunchMode launchMode) {
        Xds xds = configuration.xds;
        int port = launchMode == LaunchMode.TEST ? configuration.testPort : configuration.port;
        ServerCredentials credentials = InsecureServerCredentials.create();
        if (xds.secure) {
            credentials = XdsServerCredentials.create(credentials);
        }
        ServerBuilder<XdsServerBuilder> builder = XdsServerBuilder.forPort(port, credentials);
        // wrap with Vert.x context, so that the context interceptors work
        VertxInternal vxi = (VertxInternal) vertx;
        Executor delegate = vertx.nettyEventLoopGroup();
        ContextInternal context = vxi.createEventLoopContext();
        Executor executor = command -> delegate.execute(() -> context.dispatch(command));
        builder.executor(executor);
        // custom XDS interceptors
        if (launchMode == LaunchMode.DEVELOPMENT) {
            builder.intercept(new DevModeInterceptor(Thread.currentThread().getContextClassLoader()));
            builder.intercept(new GrpcHotReplacementInterceptor());
        }
        return builder;
    }

    @Override
    public void startServer(Server server) throws Exception {
        server.start();
    }

    @Override
    public void postStartup(Server server, ShutdownContext shutdown) {
        XdsServerReloader.init(server);
        shutdown.addShutdownTask(XdsServerReloader::reset);
    }

    @Override
    public void devModeReload(List<ServerServiceDefinition> servicesWithInterceptors,
            Map<String, ServerMethodDefinition<?, ?>> methods, List<ServerInterceptor> globalInterceptors,
            ShutdownContext shutdown) {
        XdsServerReloader.reinitialize(servicesWithInterceptors, methods, globalInterceptors);
        shutdown.addShutdownTask(XdsServerReloader::reset);
    }

    @Override
    public boolean serverAlreadyExists() {
        return XdsServerReloader.getServer() != null;
    }

    @Override
    public String serverInfo(String host, int port, GrpcServerConfiguration configuration) {
        return String.format("gRPC server on %s:%d [xDS enabled]", host, port);
    }

    @Override
    public boolean providesChannel(GrpcClientConfiguration configuration) {
        return Enabled.isEnabled(configuration.xds) || XDS.equalsIgnoreCase(configuration.nameResolver);
    }

    @Override
    public String resolver() {
        return XDS;
    }

    @Override
    public String adjustHost(String host) {
        return "/" + host;
    }

    @Override
    public ManagedChannelBuilder<?> createChannelBuilder(GrpcClientConfiguration configuration, String target) {
        ClientXds xds = configuration.xds;
        ChannelCredentials credentials = InsecureChannelCredentials.create();
        if (xds.secure) {
            credentials = XdsChannelCredentials.create(credentials);
        }
        target = xds.target.orElse(target); // use xds's target, if explicitly set
        return Grpc.newChannelBuilder(target, credentials);
    }

    @Override
    public String channelInfo(GrpcClientConfiguration configuration) {
        return "xDS";
    }
}

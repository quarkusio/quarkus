package io.quarkus.grpc.inprocess;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.quarkus.grpc.runtime.config.Enabled;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.spi.GrpcBuilderProvider;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;

public class InProcessGrpcServerBuilderProvider implements GrpcBuilderProvider<InProcessServerBuilder> {
    @Override
    public boolean providesServer(GrpcServerConfiguration configuration) {
        return Enabled.isEnabled(configuration.inProcess);
    }

    @Override
    public ServerBuilder<InProcessServerBuilder> createServerBuilder(Vertx vertx, GrpcServerConfiguration configuration,
            LaunchMode launchMode) {
        ServerBuilder<InProcessServerBuilder> builder = InProcessServerBuilder.forName(configuration.inProcess.name);
        // wrap with Vert.x context, so that the context interceptors work
        VertxInternal vxi = (VertxInternal) vertx;
        Executor delegate = vertx.nettyEventLoopGroup();
        ContextInternal context = vxi.createEventLoopContext();
        Executor executor = command -> delegate.execute(() -> context.dispatch(command));
        builder.executor(executor);
        return builder;
    }

    @Override
    public void startServer(Server server) throws Exception {
        server.start();
    }

    @Override
    public void postStartup(Server server, ShutdownContext shutdown) {
        shutdown.addShutdownTask(server::shutdownNow);
    }

    @Override
    public void devModeReload(List<ServerServiceDefinition> servicesWithInterceptors,
            Map<String, ServerMethodDefinition<?, ?>> methods, List<ServerInterceptor> globalInterceptors,
            ShutdownContext shutdown) {
        // no reload, it's in-process already
    }

    @Override
    public boolean serverAlreadyExists() {
        return false;
    }

    @Override
    public String serverInfo(String host, int port, GrpcServerConfiguration configuration) {
        return "InProcess gRPC server [" + configuration.inProcess.name + "]";
    }

    @Override
    public boolean providesChannel(GrpcClientConfiguration configuration) {
        return Enabled.isEnabled(configuration.inProcess);
    }

    @Override
    public String resolver() {
        return "in-process";
    }

    @Override
    public String adjustHost(String host) {
        return host;
    }

    @Override
    public ManagedChannelBuilder<?> createChannelBuilder(GrpcClientConfiguration configuration, String target) {
        return InProcessChannelBuilder.forName(configuration.inProcess.name).directExecutor();
    }

    @Override
    public String channelInfo(GrpcClientConfiguration configuration) {
        return "InProcess [" + configuration.inProcess.name + "]";
    }
}

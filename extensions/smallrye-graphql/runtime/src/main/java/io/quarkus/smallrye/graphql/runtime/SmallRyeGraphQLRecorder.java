package io.quarkus.smallrye.graphql.runtime;

import java.util.List;
import java.util.function.Consumer;

import graphql.schema.GraphQLSchema;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.smallrye.graphql.runtime.spi.QuarkusClassloadingService;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarNotFoundHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.smallrye.graphql.cdi.producer.GraphQLProducer;
import io.smallrye.graphql.scalar.GraphQLScalarTypes;
import io.smallrye.graphql.schema.model.Schema;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SmallRyeGraphQLRecorder {

    public RuntimeValue<Boolean> createExecutionService(BeanContainer beanContainer,
            Schema schema,
            SmallRyeGraphQLConfig graphQLConfig) {
        GraphQLProducer graphQLProducer = beanContainer.beanInstance(GraphQLProducer.class);
        if (graphQLConfig.extraScalars.isPresent()) {
            registerExtraScalars(graphQLConfig.extraScalars.get());
        }
        GraphQLSchema graphQLSchema = graphQLProducer.initialize(schema);
        return new RuntimeValue<>(graphQLSchema != null);
    }

    private void registerExtraScalars(List<ExtraScalar> extraScalars) {
        for (ExtraScalar extraScalar : extraScalars) {
            switch (extraScalar) {
                case UUID:
                    GraphQLScalarTypes.addUuid();
                    break;
            }
        }
    }

    public Handler<RoutingContext> executionHandler(RuntimeValue<Boolean> initialized, boolean allowGet,
            boolean allowPostWithQueryParameters, boolean runBlocking, boolean allowCompression) {
        if (initialized.getValue()) {
            Handler<RoutingContext> handler = new SmallRyeGraphQLExecutionHandler(allowGet,
                    allowPostWithQueryParameters, runBlocking,
                    getCurrentIdentityAssociation(),
                    Arc.container().instance(CurrentVertxRequest.class).get());
            if (allowCompression) {
                return new SmallRyeGraphQLCompressionHandler(handler);
            }
            return handler;
        } else {
            return new SmallRyeGraphQLNoEndpointHandler();
        }
    }

    public Handler<RoutingContext> graphqlOverWebsocketHandler(BeanContainer beanContainer, RuntimeValue<Boolean> initialized,
            boolean runBlocking) {
        return new SmallRyeGraphQLOverWebSocketHandler(getCurrentIdentityAssociation(),
                Arc.container().instance(CurrentVertxRequest.class).get(), runBlocking);
    }

    public Handler<RoutingContext> schemaHandler(RuntimeValue<Boolean> initialized, boolean schemaAvailable) {
        if (initialized.getValue() && schemaAvailable) {
            return new SmallRyeGraphQLSchemaHandler();
        } else {
            return new SmallRyeGraphQLNoEndpointHandler();
        }
    }

    public Handler<RoutingContext> uiHandler(String graphqlUiFinalDestination,
            String graphqlUiPath, List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations,
            SmallRyeGraphQLRuntimeConfig runtimeConfig, ShutdownContext shutdownContext) {

        if (runtimeConfig.enable) {
            WebJarStaticHandler handler = new WebJarStaticHandler(graphqlUiFinalDestination, graphqlUiPath,
                    webRootConfigurations);
            shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));
            return handler;
        } else {
            return new WebJarNotFoundHandler();
        }
    }

    public void setupClDevMode(ShutdownContext shutdownContext) {
        QuarkusClassloadingService.setClassLoader(Thread.currentThread().getContextClassLoader());
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                QuarkusClassloadingService.setClassLoader(null);
            }
        });
    }

    public Consumer<Route> routeFunction(Handler<RoutingContext> bodyHandler) {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                route.handler(bodyHandler);
            }
        };
    }

    private CurrentIdentityAssociation getCurrentIdentityAssociation() {
        InstanceHandle<CurrentIdentityAssociation> identityAssociations = Arc.container()
                .instance(CurrentIdentityAssociation.class);
        if (identityAssociations.isAvailable()) {
            return identityAssociations.get();
        }
        return null;
    }
}

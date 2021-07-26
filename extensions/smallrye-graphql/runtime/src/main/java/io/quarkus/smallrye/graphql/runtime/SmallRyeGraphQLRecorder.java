package io.quarkus.smallrye.graphql.runtime;

import java.util.function.Consumer;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import graphql.schema.GraphQLSchema;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.smallrye.graphql.runtime.spi.QuarkusClassloadingService;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.graphql.cdi.producer.GraphQLProducer;
import io.smallrye.graphql.schema.model.Schema;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SmallRyeGraphQLRecorder {

    public RuntimeValue<Boolean> createExecutionService(BeanContainer beanContainer, Schema schema) {
        GraphQLProducer graphQLProducer = beanContainer.instance(GraphQLProducer.class);
        GraphQLSchema graphQLSchema = graphQLProducer.initialize(schema);
        return new RuntimeValue<>(graphQLSchema != null);
    }

    public Handler<RoutingContext> executionHandler(RuntimeValue<Boolean> initialized, boolean allowGet,
            boolean allowPostWithQueryParameters) {
        if (initialized.getValue()) {
            return new SmallRyeGraphQLExecutionHandler(allowGet, allowPostWithQueryParameters, getCurrentIdentityAssociation(),
                    CDI.current().select(CurrentVertxRequest.class).get());
        } else {
            return new SmallRyeGraphQLNoEndpointHandler();
        }
    }

    public Handler<RoutingContext> subscriptionHandler(BeanContainer beanContainer, RuntimeValue<Boolean> initialized) {
        return new SmallRyeGraphQLSubscriptionHandler(getCurrentIdentityAssociation(),
                CDI.current().select(CurrentVertxRequest.class).get());
    }

    public Handler<RoutingContext> schemaHandler(RuntimeValue<Boolean> initialized) {
        if (initialized.getValue()) {
            return new SmallRyeGraphQLSchemaHandler();
        } else {
            return new SmallRyeGraphQLNoEndpointHandler();
        }
    }

    public Handler<RoutingContext> uiHandler(String graphqlUiFinalDestination,
            String graphqlUiPath, SmallRyeGraphQLRuntimeConfig runtimeConfig) {

        if (runtimeConfig.enable) {
            return new SmallRyeGraphQLStaticHandler(graphqlUiFinalDestination, graphqlUiPath);
        } else {
            return new SmallRyeGraphQLNotFoundHandler();
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
        Instance<CurrentIdentityAssociation> identityAssociations = CDI.current()
                .select(CurrentIdentityAssociation.class);
        if (identityAssociations.isResolvable()) {
            return identityAssociations.get();
        }
        return null;
    }
}

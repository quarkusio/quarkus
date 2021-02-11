package io.quarkus.smallrye.graphql.runtime;

import java.util.function.Function;

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
import io.smallrye.graphql.cdi.config.GraphQLConfig;
import io.smallrye.graphql.cdi.producer.GraphQLProducer;
import io.smallrye.graphql.schema.model.Schema;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SmallRyeGraphQLRecorder {

    public RuntimeValue<Boolean> createExecutionService(BeanContainer beanContainer, Schema schema) {
        GraphQLProducer graphQLProducer = beanContainer.instance(GraphQLProducer.class);
        GraphQLConfig graphQLConfig = beanContainer.instance(GraphQLConfig.class);
        GraphQLSchema graphQLSchema = graphQLProducer.initialize(schema, graphQLConfig);
        return new RuntimeValue<>(graphQLSchema != null);
    }

    public Handler<RoutingContext> executionHandler(RuntimeValue<Boolean> initialized, boolean allowGet) {
        if (initialized.getValue()) {
            Instance<CurrentIdentityAssociation> identityAssociations = CDI.current()
                    .select(CurrentIdentityAssociation.class);
            CurrentIdentityAssociation association;
            if (identityAssociations.isResolvable()) {
                association = identityAssociations.get();
            } else {
                association = null;
            }
            CurrentVertxRequest currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
            return new SmallRyeGraphQLExecutionHandler(allowGet, association, currentVertxRequest);
        } else {
            return new SmallRyeGraphQLNoEndpointHandler();
        }
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

    public Function<Router, Route> routeFunction(String rootPath, Handler<RoutingContext> bodyHandler) {
        return new Function<Router, Route>() {
            @Override
            public Route apply(Router router) {
                return router.route(rootPath).handler(bodyHandler);
            }
        };
    }
}

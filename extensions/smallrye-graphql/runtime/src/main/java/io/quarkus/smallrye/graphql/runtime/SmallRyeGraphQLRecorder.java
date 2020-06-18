package io.quarkus.smallrye.graphql.runtime;

import java.util.function.Supplier;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.smallrye.graphql.runtime.spi.QuarkusClassloadingService;
import io.quarkus.vertx.http.runtime.ThreadLocalHandler;
import io.smallrye.graphql.cdi.producer.GraphQLProducer;
import io.smallrye.graphql.schema.model.Schema;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

@Recorder
public class SmallRyeGraphQLRecorder {

    public void createExecutionService(BeanContainer beanContainer, Schema schema) {
        GraphQLProducer graphQLProducer = beanContainer.instance(GraphQLProducer.class);
        graphQLProducer.setSchema(schema);
        graphQLProducer.initialize();
    }

    public Handler<RoutingContext> executionHandler(boolean allowGet) {
        Instance<CurrentIdentityAssociation> identityAssociations = CDI.current()
                .select(CurrentIdentityAssociation.class);
        CurrentIdentityAssociation association;
        if (identityAssociations.isResolvable()) {
            association = identityAssociations.get();
        } else {
            association = null;
        }
        return new SmallRyeGraphQLExecutionHandler(allowGet, association);
    }

    public Handler<RoutingContext> schemaHandler() {
        return new SmallRyeGraphQLSchemaHandler();
    }

    public Handler<RoutingContext> uiHandler(String graphqlUiFinalDestination, String graphqlUiPath) {

        Handler<RoutingContext> handler = new ThreadLocalHandler(new Supplier<Handler<RoutingContext>>() {
            @Override
            public Handler<RoutingContext> get() {
                return StaticHandler.create().setAllowRootFileSystemAccess(true)
                        .setWebRoot(graphqlUiFinalDestination)
                        .setDefaultContentEncoding("UTF-8");
            }
        });

        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (event.normalisedPath().length() == graphqlUiPath.length()) {

                    event.response().setStatusCode(302);
                    event.response().headers().set(HttpHeaders.LOCATION, graphqlUiPath + "/");
                    event.response().end();
                    return;
                } else if (event.normalisedPath().length() == graphqlUiPath.length() + 1) {
                    event.reroute(graphqlUiPath + "/index.html");
                    return;
                }

                handler.handle(event);
            }
        };
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
}

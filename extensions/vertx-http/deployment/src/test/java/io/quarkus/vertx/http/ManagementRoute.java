package io.quarkus.vertx.http;

import java.util.function.Consumer;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public final class ManagementRoute implements Handler<RoutingContext> {
    public static Consumer<BuildChainBuilder> buildStep() {
        return builder -> builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext context) {
                NonApplicationRootPathBuildItem buildItem = context.consume(NonApplicationRootPathBuildItem.class);
                context.produce(buildItem.routeBuilder()
                        .management()
                        .route("test")
                        .handler(new ManagementRoute())
                        .blockingRoute()
                        .build());
            }
        }).produces(RouteBuildItem.class)
                .consumes(NonApplicationRootPathBuildItem.class)
                .build();
    }

    @Override
    public void handle(RoutingContext rc) {
        rc.response().end();
    }
}

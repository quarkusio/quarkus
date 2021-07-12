package io.quarkus.vertx.graphql.deployment;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.graphql.runtime.VertxGraphqlRecorder;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.WebsocketSubProtocolsBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.graphql.impl.GraphQLBatch;
// import io.vertx.ext.web.handler.graphql.impl.GraphQLInputDeserializer;
import io.vertx.ext.web.handler.graphql.impl.GraphQLQuery;

class VertxGraphqlProcessor {
    private static Pattern TRAILING_SLASH_SUFFIX_REGEX = Pattern.compile("/+$");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.VERTX_GRAPHQL);
    }

    @BuildStep
    WebsocketSubProtocolsBuildItem websocketSubProtocols() {
        return new WebsocketSubProtocolsBuildItem("graphql-ws");
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerForReflection() {
        return Arrays.asList(
                //new ReflectiveClassBuildItem(true, true, GraphQLInputDeserializer.class.getName()),
                new ReflectiveClassBuildItem(true, true, GraphQLBatch.class.getName()),
                new ReflectiveClassBuildItem(true, true, GraphQLQuery.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerVertxGraphqlUI(VertxGraphqlRecorder recorder,
            BuildProducer<NativeImageResourceDirectoryBuildItem> nativeResourcesProducer, VertxGraphqlConfig config,
            LaunchModeBuildItem launchMode,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BuildProducer<RouteBuildItem> routes,
            BodyHandlerBuildItem bodyHandler) {

        boolean includeVertxGraphqlUi = launchMode.getLaunchMode().isDevOrTest() || config.ui.alwaysInclude;
        if (!includeVertxGraphqlUi) {
            return;
        }

        Matcher matcher = TRAILING_SLASH_SUFFIX_REGEX.matcher(config.ui.path);
        String path = matcher.replaceAll("");
        if (path.isEmpty()) {
            throw new ConfigurationException(
                    "quarkus.vertx-graphql.ui.path was set to \"" + config.ui.path
                            + "\", this is not allowed as it blocks the application from serving anything else.");
        }

        Handler<RoutingContext> handler = recorder.handler();
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route(path)
                .handler(handler)
                .displayOnNotFoundPage("GraphQL UI")
                .build());
        routes.produce(
                nonApplicationRootPathBuildItem.routeBuilder()
                        .routeFunction(path + "/*", recorder.routeFunction(bodyHandler.getHandler()))
                        .handler(handler)
                        .build());

        nativeResourcesProducer.produce(new NativeImageResourceDirectoryBuildItem("io/vertx/ext/web/handler/graphiql"));
    }
}

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
import io.quarkus.deployment.builditem.nativeimage.*;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.graphql.runtime.VertxGraphqlRecorder;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.WebsocketSubProtocolsBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.graphql.impl.GraphQLBatch;
import io.vertx.ext.web.handler.graphql.impl.GraphQLQuery;

class VertxGraphqlProcessor {
    private static Pattern TRAILING_SLASH_SUFFIX_REGEX = Pattern.compile("/+$");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.VERTX_GRAPHQL);
    }

    @BuildStep
    WebsocketSubProtocolsBuildItem graphQLWSProtocol() {
        return new WebsocketSubProtocolsBuildItem("graphql-transport-ws");
    }

    @BuildStep
    WebsocketSubProtocolsBuildItem appoloWSProtocol() {
        return new WebsocketSubProtocolsBuildItem("graphql-ws");
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerForReflection() {
        return Arrays.asList(
                ReflectiveClassBuildItem.builder(GraphQLBatch.class.getName()).methods().fields().build(),
                ReflectiveClassBuildItem.builder(GraphQLQuery.class.getName()).methods().fields().build());
    }

    @BuildStep
    void registerI18nResources(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("i18n/Execution"));
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("i18n/General"));
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("i18n/Parsing"));
        resourceBundle.produce(new NativeImageResourceBundleBuildItem("i18n/Validation"));
    }

    @BuildStep
    NativeImageResourceDirectoryBuildItem produceNativeResourceDirectory(LaunchModeBuildItem launchMode,
            VertxGraphqlConfig config) {
        if (doNotIncludeVertxGraphqlUi(launchMode, config)) {
            return null;
        }
        return new NativeImageResourceDirectoryBuildItem("io/vertx/ext/web/handler/graphiql");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerVertxGraphqlUI(VertxGraphqlRecorder recorder, VertxGraphqlConfig config,
            LaunchModeBuildItem launchMode, CoreVertxBuildItem coreVertxBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BuildProducer<RouteBuildItem> routes,
            BodyHandlerBuildItem bodyHandler) {

        if (doNotIncludeVertxGraphqlUi(launchMode, config)) {
            return;
        }

        Matcher matcher = TRAILING_SLASH_SUFFIX_REGEX.matcher(config.ui.path);
        String path = matcher.replaceAll("");
        if (path.isEmpty()) {
            throw new ConfigurationException(
                    "quarkus.vertx-graphql.ui.path was set to \"" + config.ui.path
                            + "\", this is not allowed as it blocks the application from serving anything else.");
        }

        Handler<RoutingContext> handler = recorder.handler(coreVertxBuildItem.getVertx());
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
    }

    private static boolean doNotIncludeVertxGraphqlUi(LaunchModeBuildItem launchMode, VertxGraphqlConfig config) {
        return !launchMode.getLaunchMode().isDevOrTest() && !config.ui.alwaysInclude;
    }
}

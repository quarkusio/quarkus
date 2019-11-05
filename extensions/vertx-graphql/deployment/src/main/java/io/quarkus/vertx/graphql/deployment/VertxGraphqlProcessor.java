package io.quarkus.vertx.graphql.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.WebsocketSubProtocolsBuildItem;

class VertxGraphqlProcessor {

    private static final String FEATURE = "vertx-graphql";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    WebsocketSubProtocolsBuildItem websocketSubProtocols(CombinedIndexBuildItem combinedIndexBuildItem) {
        return new WebsocketSubProtocolsBuildItem("graphql-ws");
    }
}

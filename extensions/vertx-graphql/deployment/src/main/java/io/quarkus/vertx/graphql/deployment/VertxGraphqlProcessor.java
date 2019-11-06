package io.quarkus.vertx.graphql.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.WebsocketSubProtocolsBuildItem;

class VertxGraphqlProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.VERTX_GRAPHQL);
    }

    @BuildStep
    WebsocketSubProtocolsBuildItem websocketSubProtocols() {
        return new WebsocketSubProtocolsBuildItem("graphql-ws");
    }
}

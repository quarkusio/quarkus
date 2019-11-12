package io.quarkus.vertx.graphql.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImagePackageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.vertx.http.deployment.WebsocketSubProtocolsBuildItem;
import io.vertx.ext.web.handler.graphql.impl.GraphQLBatch;
import io.vertx.ext.web.handler.graphql.impl.GraphQLInputDeserializer;
import io.vertx.ext.web.handler.graphql.impl.GraphQLQuery;

class VertxGraphqlProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.VERTX_GRAPHQL);
    }

    @BuildStep
    WebsocketSubProtocolsBuildItem websocketSubProtocols() {
        return new WebsocketSubProtocolsBuildItem("graphql-ws");
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerForReflection() {
        return Arrays.asList(
                new ReflectiveClassBuildItem(true, true, GraphQLInputDeserializer.class.getName()),
                new ReflectiveClassBuildItem(true, true, GraphQLBatch.class.getName()),
                new ReflectiveClassBuildItem(true, true, GraphQLQuery.class.getName()));
    }

    @BuildStep
    NativeImagePackageResourceBuildItem registerNativeImageResources() {
        return new NativeImagePackageResourceBuildItem("io/vertx/ext/web/handler/graphiql");
    }
}

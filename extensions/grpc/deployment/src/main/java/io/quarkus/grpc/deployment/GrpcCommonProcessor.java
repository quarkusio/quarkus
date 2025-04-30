package io.quarkus.grpc.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

/**
 * A processor used for both client and server
 */
public class GrpcCommonProcessor {

    /**
     * Index the gRPC stubs.
     * This is used to allows application find the classes generated from the proto file included in the dependency at build
     * time.
     * <p>
     * See <a href="https://github.com/quarkusio/quarkus/issues/37312">#37312</a>
     */
    @BuildStep
    void indexGrpcStub(BuildProducer<IndexDependencyBuildItem> index) {
        index.produce(new IndexDependencyBuildItem("io.quarkus", "quarkus-grpc-stubs"));
    }
}

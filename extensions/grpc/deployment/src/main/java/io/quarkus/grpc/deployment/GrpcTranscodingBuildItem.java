package io.quarkus.grpc.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.grpc.transcoding.GrpcTranscodingMethod;

public final class GrpcTranscodingBuildItem extends MultiBuildItem {

    final DotName marshallingClass;
    final List<GrpcTranscodingMethod> transcodingMethods;

    public GrpcTranscodingBuildItem(DotName marshallingClass, List<GrpcTranscodingMethod> transcodingMethods) {
        this.marshallingClass = marshallingClass;
        this.transcodingMethods = transcodingMethods;
    }

    public DotName getMarshallingClass() {
        return marshallingClass;
    }

    public List<GrpcTranscodingMethod> getTranscodingMethods() {
        return transcodingMethods;
    }
}

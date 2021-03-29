package io.quarkus.grpc.common.deployment;

import org.jboss.jandex.DotName;

import com.google.protobuf.GeneratedMessageV3;

import io.grpc.LoadBalancerProvider;
import io.grpc.NameResolverProvider;

public class GrpcDotNames {
    static final DotName MESSAGE_BUILDER = DotName.createSimple(GeneratedMessageV3.Builder.class.getName());
    static final DotName GENERATED_MESSAGE_V3 = DotName.createSimple(GeneratedMessageV3.class.getName());
    static final DotName NAME_RESOLVER_PROVIDER = DotName.createSimple(NameResolverProvider.class.getName());
    static final DotName LOAD_BALANCER_PROVIDER = DotName.createSimple(LoadBalancerProvider.class.getName());
}

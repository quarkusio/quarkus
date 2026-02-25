package io.quarkus.grpc.common.deployment;

import org.jboss.jandex.DotName;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.ProtocolMessageEnum;

import io.grpc.LoadBalancerProvider;
import io.grpc.NameResolverProvider;

public class GrpcDotNames {
    static final DotName GENERATED_MESSAGE = DotName.createSimple(GeneratedMessage.class.getName());
    static final DotName GENERATED_MESSAGE_V3 = DotName.createSimple(GeneratedMessageV3.class.getName());
    static final DotName MESSAGE_BUILDER = DotName.createSimple(GeneratedMessage.Builder.class.getName());
    static final DotName MESSAGE_BUILDER_V3 = DotName.createSimple(GeneratedMessageV3.Builder.class.getName());
    static final DotName PROTOCOL_MESSAGE_ENUM = DotName.createSimple(ProtocolMessageEnum.class.getName());
    static final DotName DESCRIPTOR_PROTOS = DotName.createSimple(DescriptorProtos.class.getName());
    static final DotName NAME_RESOLVER_PROVIDER = DotName.createSimple(NameResolverProvider.class.getName());
    static final DotName LOAD_BALANCER_PROVIDER = DotName.createSimple(LoadBalancerProvider.class.getName());
}

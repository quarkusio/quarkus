package io.quarkus.grpc.deployment;

import org.jboss.jandex.DotName;

import com.google.protobuf.GeneratedMessageV3;

import io.grpc.Channel;
import io.grpc.LoadBalancerProvider;
import io.grpc.NameResolverProvider;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.quarkus.grpc.runtime.supports.Channels;

public class GrpcDotNames {

    static final DotName CHANNEL = DotName.createSimple(Channel.class.getName());
    static final DotName GRPC_SERVICE = DotName.createSimple(GrpcService.class.getName());
    static final DotName MESSAGE_BUILDER = DotName.createSimple(GeneratedMessageV3.Builder.class.getName());
    static final DotName GENERATED_MESSAGE_V3 = DotName.createSimple(GeneratedMessageV3.class.getName());
    static final DotName NAME_RESOLVER_PROVIDER = DotName.createSimple(NameResolverProvider.class.getName());
    static final DotName LOAD_BALANCER_PROVIDER = DotName.createSimple(LoadBalancerProvider.class.getName());

    static final MethodDescriptor CREATE_CHANNEL_METHOD = MethodDescriptor.ofMethod(Channels.class, "createChannel",
            Channel.class, String.class);
    static final MethodDescriptor RETRIEVE_CHANNEL_METHOD = MethodDescriptor.ofMethod(Channels.class, "retrieveChannel",
            Channel.class, String.class);

}

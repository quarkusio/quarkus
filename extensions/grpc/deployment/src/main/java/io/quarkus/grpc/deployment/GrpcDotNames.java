package io.quarkus.grpc.deployment;

import org.jboss.jandex.DotName;

import io.grpc.BindableService;
import io.grpc.Channel;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.quarkus.grpc.runtime.supports.Channels;
import io.smallrye.common.annotation.Blocking;

public class GrpcDotNames {

    static final DotName BINDABLE_SERVICE = DotName.createSimple(BindableService.class.getName());
    static final DotName CHANNEL = DotName.createSimple(Channel.class.getName());
    static final DotName GRPC_SERVICE = DotName.createSimple(GrpcService.class.getName());

    static final DotName BLOCKING = DotName.createSimple(Blocking.class.getName());

    static final MethodDescriptor CREATE_CHANNEL_METHOD = MethodDescriptor.ofMethod(Channels.class, "createChannel",
            Channel.class, String.class);
    static final MethodDescriptor RETRIEVE_CHANNEL_METHOD = MethodDescriptor.ofMethod(Channels.class, "retrieveChannel",
            Channel.class, String.class);

}

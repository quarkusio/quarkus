package io.quarkus.grpc.deployment;

import org.jboss.jandex.DotName;

import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.runtime.MutinyStub;
import io.quarkus.grpc.runtime.supports.Channels;
import io.quarkus.grpc.runtime.supports.GrpcClientConfigProvider;
import io.smallrye.common.annotation.Blocking;

public class GrpcDotNames {

    static final DotName BINDABLE_SERVICE = DotName.createSimple(BindableService.class.getName());
    static final DotName CHANNEL = DotName.createSimple(Channel.class.getName());
    static final DotName GRPC_CLIENT = DotName.createSimple(GrpcClient.class.getName());
    static final DotName GRPC_SERVICE = DotName.createSimple(GrpcService.class.getName());

    static final DotName BLOCKING = DotName.createSimple(Blocking.class.getName());

    static final DotName ABSTRACT_BLOCKING_STUB = DotName.createSimple(AbstractBlockingStub.class.getName());
    static final DotName MUTINY_STUB = DotName.createSimple(MutinyStub.class.getName());

    static final MethodDescriptor CREATE_CHANNEL_METHOD = MethodDescriptor.ofMethod(Channels.class, "createChannel",
            Channel.class, String.class);
    static final MethodDescriptor RETRIEVE_CHANNEL_METHOD = MethodDescriptor.ofMethod(Channels.class, "retrieveChannel",
            Channel.class, String.class);

    static final MethodDescriptor CONFIGURE_STUB = MethodDescriptor.ofMethod(GrpcClientConfigProvider.class,
            "configureStub", AbstractStub.class, String.class, AbstractStub.class);

}

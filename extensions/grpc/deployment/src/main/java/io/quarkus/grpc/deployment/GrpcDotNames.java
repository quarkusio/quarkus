package io.quarkus.grpc.deployment;

import java.util.function.BiFunction;

import org.jboss.jandex.DotName;

import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.runtime.MutinyBean;
import io.quarkus.grpc.runtime.MutinyClient;
import io.quarkus.grpc.runtime.MutinyGrpc;
import io.quarkus.grpc.runtime.MutinyService;
import io.quarkus.grpc.runtime.MutinyStub;
import io.quarkus.grpc.runtime.supports.Channels;
import io.quarkus.grpc.runtime.supports.GrpcClientConfigProvider;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

public class GrpcDotNames {

    public static final DotName BINDABLE_SERVICE = DotName.createSimple(BindableService.class.getName());
    public static final DotName CHANNEL = DotName.createSimple(Channel.class.getName());
    public static final DotName GRPC_CLIENT = DotName.createSimple(GrpcClient.class.getName());
    public static final DotName GRPC_SERVICE = DotName.createSimple(GrpcService.class.getName());

    public static final DotName BLOCKING = DotName.createSimple(Blocking.class.getName());
    public static final DotName NON_BLOCKING = DotName.createSimple(NonBlocking.class.getName());

    public static final DotName ABSTRACT_BLOCKING_STUB = DotName.createSimple(AbstractBlockingStub.class.getName());
    public static final DotName MUTINY_STUB = DotName.createSimple(MutinyStub.class.getName());
    public static final DotName MUTINY_GRPC = DotName.createSimple(MutinyGrpc.class.getName());
    public static final DotName MUTINY_CLIENT = DotName.createSimple(MutinyClient.class.getName());
    public static final DotName MUTINY_BEAN = DotName.createSimple(MutinyBean.class.getName());
    public static final DotName MUTINY_SERVICE = DotName.createSimple(MutinyService.class.getName());

    static final MethodDescriptor CREATE_CHANNEL_METHOD = MethodDescriptor.ofMethod(Channels.class, "createChannel",
            Channel.class, String.class);
    static final MethodDescriptor RETRIEVE_CHANNEL_METHOD = MethodDescriptor.ofMethod(Channels.class, "retrieveChannel",
            Channel.class, String.class);

    static final MethodDescriptor CONFIGURE_STUB = MethodDescriptor.ofMethod(GrpcClientConfigProvider.class,
            "configureStub", AbstractStub.class, String.class, AbstractStub.class);
    static final MethodDescriptor ADD_BLOCKING_CLIENT_INTERCEPTOR = MethodDescriptor.ofMethod(GrpcClientConfigProvider.class,
            "addBlockingClientInterceptor", AbstractStub.class, AbstractStub.class);
    static final MethodDescriptor GET_STUB_CONFIGURATOR = MethodDescriptor.ofMethod(GrpcClientConfigProvider.class,
            "getStubConfigurator", BiFunction.class);

}

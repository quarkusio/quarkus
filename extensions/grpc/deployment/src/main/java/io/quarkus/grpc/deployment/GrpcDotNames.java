package io.quarkus.grpc.deployment;

import java.util.Set;
import java.util.function.BiFunction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.MutinyBean;
import io.quarkus.grpc.MutinyClient;
import io.quarkus.grpc.MutinyGrpc;
import io.quarkus.grpc.MutinyService;
import io.quarkus.grpc.MutinyStub;
import io.quarkus.grpc.RegisterClientInterceptor;
import io.quarkus.grpc.RegisterInterceptor;
import io.quarkus.grpc.RegisterInterceptors;
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
    public static final DotName TRANSACTIONAL = DotName.createSimple("jakarta.transaction.Transactional");

    public static final DotName ABSTRACT_BLOCKING_STUB = DotName.createSimple(AbstractBlockingStub.class.getName());
    public static final DotName MUTINY_STUB = DotName.createSimple(MutinyStub.class.getName());
    public static final DotName MUTINY_GRPC = DotName.createSimple(MutinyGrpc.class.getName());
    public static final DotName MUTINY_CLIENT = DotName.createSimple(MutinyClient.class.getName());
    public static final DotName MUTINY_BEAN = DotName.createSimple(MutinyBean.class.getName());
    public static final DotName MUTINY_SERVICE = DotName.createSimple(MutinyService.class.getName());

    public static final DotName GLOBAL_INTERCEPTOR = DotName.createSimple(GlobalInterceptor.class.getName());
    public static final DotName REGISTER_INTERCEPTOR = DotName.createSimple(RegisterInterceptor.class.getName());
    public static final DotName REGISTER_INTERCEPTORS = DotName.createSimple(RegisterInterceptors.class.getName());
    public static final DotName SERVER_INTERCEPTOR = DotName.createSimple(ServerInterceptor.class.getName());
    public static final DotName REGISTER_CLIENT_INTERCEPTOR = DotName.createSimple(RegisterClientInterceptor.class.getName());
    public static final DotName REGISTER_CLIENT_INTERCEPTOR_LIST = DotName
            .createSimple(RegisterClientInterceptor.List.class.getName());
    public static final DotName CLIENT_INTERCEPTOR = DotName.createSimple(ClientInterceptor.class.getName());

    static final MethodDescriptor CREATE_CHANNEL_METHOD = MethodDescriptor.ofMethod(Channels.class, "createChannel",
            Channel.class, String.class, Set.class);
    static final MethodDescriptor RETRIEVE_CHANNEL_METHOD = MethodDescriptor.ofMethod(Channels.class, "retrieveChannel",
            Channel.class, String.class, Set.class);

    static final MethodDescriptor CONFIGURE_STUB = MethodDescriptor.ofMethod(GrpcClientConfigProvider.class,
            "configureStub", AbstractStub.class, String.class, AbstractStub.class);
    static final MethodDescriptor ADD_BLOCKING_CLIENT_INTERCEPTOR = MethodDescriptor.ofMethod(GrpcClientConfigProvider.class,
            "addBlockingClientInterceptor", AbstractStub.class, AbstractStub.class);
    static final MethodDescriptor GET_STUB_CONFIGURATOR = MethodDescriptor.ofMethod(GrpcClientConfigProvider.class,
            "getStubConfigurator", BiFunction.class);

    static boolean isGrpcClient(AnnotationInstance instance) {
        return instance.name().equals(GRPC_CLIENT);
    }

}

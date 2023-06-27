package io.quarkus.grpc.examples.interceptors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;

class ServerInterceptors {
    @GlobalInterceptor
    @ApplicationScoped
    static class TypeTarget extends Base {
    }

    static class MethodTarget extends Base {
    }

    static class Producer {
        @GlobalInterceptor
        @Produces
        MethodTarget methodTarget() {
            return new MethodTarget();
        }
    }

    abstract static class Base implements ServerInterceptor {
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata metadata,
                ServerCallHandler<ReqT, RespT> next) {
            HelloWorldEndpoint.invoked.add(getClass().getName());
            return next.startCall(call, metadata);
        }
    }
}

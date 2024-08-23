package io.quarkus.grpc.examples.interceptors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import io.quarkus.grpc.GlobalInterceptor;

class ClientInterceptors {
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

    abstract static class Base implements ClientInterceptor {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions options,
                Channel next) {
            HelloWorldEndpoint.invoked.add(getClass().getName());
            return next.newCall(method, options);
        }
    }
}

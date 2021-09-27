package io.quarkus.grpc.server.devmode;

import javax.enterprise.context.ApplicationScoped;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;

@ApplicationScoped
@GlobalInterceptor
public class DevModeTestInterceptor implements ServerInterceptor {

    private volatile String lastStatus = "initial";

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
            Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        return serverCallHandler
                .startCall(new ForwardingServerCall.SimpleForwardingServerCall<>(serverCall) {
                    @Override
                    protected ServerCall<ReqT, RespT> delegate() {
                        lastStatus = getStatus();
                        return super.delegate();
                    }
                }, metadata);
    }

    public String getLastStatus() {
        return lastStatus;
    }

    private String getStatus() {
        return "status";
    }
}

package io.quarkus.grpc.runtime;

import java.util.Set;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GrpcClientRecorder {

    public RuntimeValue<ClientInterceptorStorage> initClientInterceptorStorage(Set<Class<?>> perClientInterceptors,
            Set<Class<?>> globalInterceptors) {
        return new RuntimeValue<>(new ClientInterceptorStorage(perClientInterceptors, globalInterceptors));
    }

}

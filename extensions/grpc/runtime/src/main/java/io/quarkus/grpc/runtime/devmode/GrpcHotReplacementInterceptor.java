package io.quarkus.grpc.runtime.devmode;

import java.util.function.Supplier;

public class GrpcHotReplacementInterceptor {
    private static volatile Supplier<Boolean> interceptorAction;

    static void register(Supplier<Boolean> onCall) {
        interceptorAction = onCall;
    }

    public static boolean fire() {
        return interceptorAction.get();
    }
}

package io.quarkus.amazon.lambda.test;

import io.quarkus.test.common.NativeImageStartedNotifier;

public class LambdaStartedNotifier implements NativeImageStartedNotifier {

    static volatile boolean started = false;

    @Override
    public boolean isNativeImageStarted() {
        return started;
    }
}

package io.sentry.jvmti;

public class ResetFrameCache {

    public static void resetFrameCache() {
        FrameCache.reset();
    }
}

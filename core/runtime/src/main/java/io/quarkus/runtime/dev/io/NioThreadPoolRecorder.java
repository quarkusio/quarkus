package io.quarkus.runtime.dev.io;

import io.quarkus.dev.io.NioThreadPoolThreadFactory;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NioThreadPoolRecorder {

    public void updateTccl(ShutdownContext context) {
        ClassLoader newTccl = Thread.currentThread().getContextClassLoader();
        ClassLoader oldTccl = NioThreadPoolThreadFactory.updateTccl(newTccl);
        if (newTccl != oldTccl) {
            context.addLastShutdownTask(new Runnable() {
                @Override
                public void run() {
                    NioThreadPoolThreadFactory.updateTccl(oldTccl);
                }
            });
        }
        // Else: don't add an unnecessary shutdown task that may hold a reference to a QuarkusClassLoader,
        // which could be a problem with QuarkusUnitTest since it creates one classloader per test.
    }
}

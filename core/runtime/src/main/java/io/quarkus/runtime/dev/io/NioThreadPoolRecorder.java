package io.quarkus.runtime.dev.io;

import io.quarkus.dev.io.NioThreadPoolThreadFactory;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NioThreadPoolRecorder {

    public void updateTccl(ShutdownContext context) {
        ClassLoader old = NioThreadPoolThreadFactory.updateTccl(Thread.currentThread().getContextClassLoader());
        context.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                NioThreadPoolThreadFactory.updateTccl(old);
            }
        });
    }
}

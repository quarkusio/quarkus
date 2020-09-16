package io.quarkus.jdbc.mysql.runtime;

import org.graalvm.nativeimage.ImageInfo;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

import io.quarkus.jdbc.mysql.runtime.graal.AbandonedConnectionCleanupThreadSubstitutions;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MySQLRecorder {

    public void startAbandonedConnectionCleanup(ShutdownContext shutdownContext) {
        if (ImageInfo.inImageRuntimeCode()) {
            AbandonedConnectionCleanupThreadSubstitutions.startCleanUp();
        }
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                AbandonedConnectionCleanupThread.uncheckedShutdown();
            }
        });
    }
}

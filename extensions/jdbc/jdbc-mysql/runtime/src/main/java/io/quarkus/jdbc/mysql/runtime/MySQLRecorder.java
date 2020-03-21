package io.quarkus.jdbc.mysql.runtime;

import org.graalvm.nativeimage.ImageInfo;

import io.quarkus.jdbc.mysql.runtime.graal.AbandonedConnectionCleanupThreadSubstitutions;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MySQLRecorder {

    public void startAbandonedConnectionCleanup() {
        if (ImageInfo.inImageRuntimeCode()) {
            AbandonedConnectionCleanupThreadSubstitutions.startCleanUp();
        }
    }
}

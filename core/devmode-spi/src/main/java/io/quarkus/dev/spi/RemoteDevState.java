package io.quarkus.dev.spi;

import java.io.Serializable;
import java.util.Map;

public class RemoteDevState implements Serializable {
    final Map<String, String> fileHashes;
    final Throwable throwable;

    public RemoteDevState(Map<String, String> fileHashes, Throwable throwable) {
        this.fileHashes = fileHashes;
        this.throwable = throwable;
    }

    public Map<String, String> getFileHashes() {
        return fileHashes;
    }

    public Throwable getAugmentProblem() {
        return throwable;
    }
}

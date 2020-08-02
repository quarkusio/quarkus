package io.quarkus.deployment.dev.remote;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.dev.spi.RemoteDevState;

public interface RemoteDevClient {

    Closeable sendConnectRequest(RemoteDevState initialState,
            Function<Set<String>, Map<String, byte[]>> initialConnectFunction, Supplier<SyncResult> changeRequestFunction);

    interface SyncResult {
        Map<String, byte[]> getChangedFiles();

        Set<String> getRemovedFiles();

        Throwable getProblem();
    }

}

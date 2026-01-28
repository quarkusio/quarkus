package io.quarkus.aesh.websocket.runtime;

import java.util.List;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AeshWebSocketRecorder {

    public Supplier<AeshWebSocketSecurityCheck> createSecurityCheck(List<String> rolesAllowed) {
        return () -> new AeshWebSocketSecurityCheck(rolesAllowed);
    }

    public void setWebSocketPath(String path) {
        AeshWebSocketPath.setPath(path);
    }
}

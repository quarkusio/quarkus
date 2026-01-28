package io.quarkus.aesh.websocket.runtime;

import java.util.List;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AeshWebSocketRecorder {

    public Supplier<AeshWebSocketSecurityCheck> createSecurityCheck(List<String> rolesAllowed,
            boolean requireAuthenticated) {
        return () -> new AeshWebSocketSecurityCheck(rolesAllowed, requireAuthenticated);
    }

    public void setWebSocketPath(String path) {
        AeshWebSocketPath.setPath(path);
    }
}

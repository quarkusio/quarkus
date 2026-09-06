package io.quarkus.websockets.next.runtime.produi;

import java.util.List;
import java.util.Map;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class WebSocketsNextProdUIRecorder {

    public void initializeProdUIService(List<Map<String, Object>> endpoints) {
        InstanceHandle<WebSocketsNextProdUIService> handle = Arc.container().instance(WebSocketsNextProdUIService.class);
        if (handle.isAvailable()) {
            handle.get().setEndpoints(endpoints);
        }
    }
}

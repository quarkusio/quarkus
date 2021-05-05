package io.quarkus.grpc.runtime.devmode;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GrpcDevConsoleRecorder {

    public void setServerConfiguration() {
        try (InstanceHandle<GrpcConfiguration> config = Arc.container().instance(GrpcConfiguration.class)) {
            GrpcServerConfiguration serverConfig = config.get().server;
            Map<String, Object> map = new HashMap<>();
            map.put("host", serverConfig.host);
            map.put("port", serverConfig.port);
            map.put("ssl", serverConfig.ssl.certificate.isPresent() || serverConfig.ssl.keyStore.isPresent());
            DevConsoleManager.setGlobal("io.quarkus.grpc.serverConfig", map);
        }
    }

}

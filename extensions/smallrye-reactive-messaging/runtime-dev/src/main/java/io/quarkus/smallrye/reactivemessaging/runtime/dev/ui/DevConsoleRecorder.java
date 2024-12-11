package io.quarkus.smallrye.reactivemessaging.runtime.dev.ui;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DevConsoleRecorder {

    static final Map<String, String> EMITTERS = new HashMap<>();
    static final Map<String, String> CHANNELS = new HashMap<>();

    public void setInjectionInfo(Map<String, String> emitters, Map<String, String> channels) {
        EMITTERS.putAll(emitters);
        CHANNELS.putAll(channels);
    }

}

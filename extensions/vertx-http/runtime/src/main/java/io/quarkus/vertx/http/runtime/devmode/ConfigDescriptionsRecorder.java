package io.quarkus.vertx.http.runtime.devmode;

import java.util.List;
import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConfigDescriptionsRecorder {

    public ConfigDescriptionsManager manager(List<ConfigDescription> descriptions, Set<String> devServicesProperties) {
        return new ConfigDescriptionsManager(descriptions, devServicesProperties);
    }
}

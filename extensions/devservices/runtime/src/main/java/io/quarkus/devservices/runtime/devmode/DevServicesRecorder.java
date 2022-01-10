package io.quarkus.devservices.runtime.devmode;

import java.util.List;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DevServicesRecorder {

    public DevServices devServices(List<DevServiceDescription> devServices) {
        return new DevServices(devServices);
    }

}

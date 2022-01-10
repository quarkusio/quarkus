package io.quarkus.devservices.runtime.devmode;

import java.util.List;
import java.util.function.Supplier;

public class DevServices implements Supplier<List<DevServiceDescription>> {

    private List<DevServiceDescription> devServices;

    public DevServices() {
    }

    public DevServices(List<DevServiceDescription> devServices) {
        this.devServices = devServices;
    }

    public List<DevServiceDescription> getDevServices() {
        return devServices;
    }

    @Override
    public List<DevServiceDescription> get() {
        return devServices;
    }
}

package io.quarkus.stork;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.config.ConfigProvider;

public class StorkConfigProvider implements ConfigProvider {

    private static final List<ServiceConfig> serviceConfigs = new ArrayList<>();

    public static void init(List<ServiceConfig> configs) {
        serviceConfigs.clear();
        serviceConfigs.addAll(configs);
    }

    @Override
    public List<ServiceConfig> getConfigs() {
        return serviceConfigs;
    }

    @Override
    public int priority() {
        return 150;
    }

}

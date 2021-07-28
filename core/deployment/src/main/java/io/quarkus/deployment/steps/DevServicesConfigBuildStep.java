package io.quarkus.deployment.steps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;

class DevServicesConfigBuildStep {
    static volatile Map<String, String> oldConfig;

    @BuildStep
    List<DevServicesConfigResultBuildItem> deprecated(List<DevServicesNativeConfigResultBuildItem> items) {
        return items.stream().map(s -> new DevServicesConfigResultBuildItem(s.getKey(), s.getValue()))
                .collect(Collectors.toList());
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    DevServicesLauncherConfigResultBuildItem setup(BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfig,
            List<DevServicesConfigResultBuildItem> devServicesConfigResultBuildItems) {
        Map<String, String> newProperties = new HashMap<>(devServicesConfigResultBuildItems.stream().collect(
                Collectors.toMap(DevServicesConfigResultBuildItem::getKey, DevServicesConfigResultBuildItem::getValue)));
        Config config = ConfigProvider.getConfig();
        //check if there are existing already started dev services
        //if there were no changes to the processors they don't produce config
        //so we merge existing config from previous runs
        //we also check the current config, as the dev service may have been disabled by explicit config
        if (oldConfig != null) {
            for (Map.Entry<String, String> entry : oldConfig.entrySet()) {
                if (!newProperties.containsKey(entry.getKey())
                        && config.getOptionalValue(entry.getKey(), String.class).isEmpty()) {
                    newProperties.put(entry.getKey(), entry.getValue());
                }
            }
        }
        for (Map.Entry<String, String> entry : newProperties.entrySet()) {
            runtimeConfig.produce(new RunTimeConfigurationDefaultBuildItem(entry.getKey(), entry.getValue()));
        }
        oldConfig = newProperties;
        return new DevServicesLauncherConfigResultBuildItem(Collections.unmodifiableMap(newProperties));
    }
}

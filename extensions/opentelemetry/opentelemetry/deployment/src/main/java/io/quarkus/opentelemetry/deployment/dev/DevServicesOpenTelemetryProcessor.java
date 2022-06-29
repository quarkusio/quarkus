package io.quarkus.opentelemetry.deployment.dev;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;

import io.quarkus.datasource.deployment.spi.DevServicesDatasourceResultBuildItem;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;

public class DevServicesOpenTelemetryProcessor {

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = { OpenTelemetryEnabled.class, GlobalDevServicesConfig.Enabled.class })
    void devServicesDatasources(
            Optional<DevServicesDatasourceResultBuildItem> devServicesDatasources,
            BuildProducer<DevServicesAdditionalConfigBuildItem> devServicesAdditionalConfig) {

        if (devServicesDatasources.isPresent()) {
            Map<String, String> properties = new HashMap<>();
            DevServicesDatasourceResultBuildItem.DbResult defaultDatasource = devServicesDatasources.get()
                    .getDefaultDatasource();
            if (defaultDatasource != null) {
                properties.putAll(defaultDatasource.getConfigProperties());
            }

            Map<String, DevServicesDatasourceResultBuildItem.DbResult> namedDatasources = devServicesDatasources.get()
                    .getNamedDatasources();
            if (namedDatasources != null) {
                for (DevServicesDatasourceResultBuildItem.DbResult dbResult : namedDatasources.values()) {
                    if (dbResult != null) {
                        properties.putAll(dbResult.getConfigProperties());
                    }
                }
            }

            // if we find a dev service datasource and the OTel driver, we rewrite the jdbc url to add the otel prefix
            for (Map.Entry<String, String> property : properties.entrySet()) {
                String key = property.getKey();
                String value = property.getValue();
                if (key.endsWith(".url") && value.startsWith("jdbc:")) {
                    String driverKey = key.substring(0, key.length() - 4) + ".driver";
                    ConfigValue driverValue = ConfigProvider.getConfig().getConfigValue(driverKey);
                    if (driverValue.getValue() != null
                            && driverValue.getValue().equals("io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver")) {
                        devServicesAdditionalConfig.produce(new DevServicesAdditionalConfigBuildItem(key, key,
                                value.replaceFirst("jdbc:", "jdbc:otel:"), () -> {
                                }));
                    }
                }
            }
        }
    }
}

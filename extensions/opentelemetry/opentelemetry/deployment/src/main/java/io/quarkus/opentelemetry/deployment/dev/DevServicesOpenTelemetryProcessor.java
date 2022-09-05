package io.quarkus.opentelemetry.deployment.dev;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { OpenTelemetryEnabled.class, GlobalDevServicesConfig.Enabled.class })
public class DevServicesOpenTelemetryProcessor {

    @BuildStep
    void devServicesDatasources(List<JdbcDataSourceBuildItem> jdbcDataSources,
            BuildProducer<DevServicesAdditionalConfigBuildItem> devServicesAdditionalConfig) {
        for (JdbcDataSourceBuildItem dataSource : jdbcDataSources) {
            // if the datasource is explicitly configured to use the OTel driver...
            if (dataSourceUsesOTelJdbcDriver(dataSource.getName())) {
                List<String> urlPropertyKeys = DataSourceUtil.dataSourcePropertyKeys(dataSource.getName(), "jdbc.url");
                devServicesAdditionalConfig.produce(new DevServicesAdditionalConfigBuildItem(devServicesConfig -> {
                    Map<String, String> overrides = new HashMap<>();
                    for (String key : urlPropertyKeys) {
                        String devServicesUrl = devServicesConfig.get(key);
                        // ... and if the same datasource uses dev services...
                        if (devServicesUrl != null) {
                            // ... then we rewrite the jdbc url to add the otel prefix.
                            overrides.put(key, devServicesUrl.replaceFirst("jdbc:", "jdbc:otel:"));
                        }
                    }
                    return overrides;
                }));
            }
        }
    }

    private boolean dataSourceUsesOTelJdbcDriver(String dataSourceName) {
        List<String> driverPropertyKeys = DataSourceUtil.dataSourcePropertyKeys(dataSourceName, "jdbc.driver");
        for (String driverPropertyKey : driverPropertyKeys) {
            ConfigValue explicitlyConfiguredDriverValue = ConfigProvider.getConfig().getConfigValue(driverPropertyKey);
            if (explicitlyConfiguredDriverValue.getValue() != null) {
                return explicitlyConfiguredDriverValue.getValue()
                        .equals("io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver");
            }
        }
        return false;
    }
}

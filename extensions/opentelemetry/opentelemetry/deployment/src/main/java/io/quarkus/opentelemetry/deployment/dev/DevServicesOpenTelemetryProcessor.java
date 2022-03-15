package io.quarkus.opentelemetry.deployment.dev;

import static io.quarkus.opentelemetry.runtime.dev.OpenTelemetryDevServicesConfigBuilder.OPENTELEMETRY_DEVSERVICES_CONFIG;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;

import io.quarkus.datasource.deployment.spi.DevServicesDatasourceResultBuildItem;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;
import io.quarkus.opentelemetry.runtime.dev.OpenTelemetryDevServicesConfigBuilder;

public class DevServicesOpenTelemetryProcessor {

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = { OpenTelemetryEnabled.class, GlobalDevServicesConfig.Enabled.class })
    void devServicesDatasources(
            Optional<DevServicesDatasourceResultBuildItem> devServicesDatasources,
            BuildProducer<GeneratedResourceBuildItem> generatedResource,
            BuildProducer<RunTimeConfigBuilderBuildItem> runtimeConfigBuilder) throws Exception {

        if (devServicesDatasources.isPresent()) {
            Properties properties = new Properties();
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
            for (Map.Entry<Object, Object> property : properties.entrySet()) {
                String key = (String) property.getKey();
                String value = (String) property.getValue();
                if (key.endsWith(".url") && value.startsWith("jdbc:")) {
                    String driverKey = key.substring(0, key.length() - 4) + ".driver";
                    ConfigValue driverValue = ConfigProvider.getConfig().getConfigValue(driverKey);
                    if (driverValue.getValue() != null
                            && driverValue.getValue().equals("io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver")) {
                        properties.put(key, value.replaceFirst("jdbc:", "jdbc:otel:"));
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            properties.store(out, null);

            // write the config with a slightly higher priority then runtime defaults
            // see io.quarkus.deployment.steps.ConfigGenerationBuildStep#runtimeDefaultsConfig
            generatedResource
                    .produce(new GeneratedResourceBuildItem(OPENTELEMETRY_DEVSERVICES_CONFIG, out.toByteArray()));
            runtimeConfigBuilder
                    .produce(new RunTimeConfigBuilderBuildItem(OpenTelemetryDevServicesConfigBuilder.class.getName()));
        }
    }
}

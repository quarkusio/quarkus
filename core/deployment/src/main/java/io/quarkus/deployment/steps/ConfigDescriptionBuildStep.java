package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.configuration.matching.ConfigPatternMap;
import io.quarkus.deployment.configuration.matching.Container;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.util.ClassPathUtils;

public class ConfigDescriptionBuildStep {

    @BuildStep
    List<ConfigDescriptionBuildItem> createConfigDescriptions(
            ConfigurationBuildItem config) throws Exception {
        Properties javadoc = new Properties();
        ClassPathUtils.consumeAsStreams("META-INF/quarkus-javadoc.properties", in -> {
            try {
                javadoc.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        List<ConfigDescriptionBuildItem> ret = new ArrayList<>();
        processConfig(config.getReadResult().getBuildTimePatternMap(), ret, javadoc);
        processConfig(config.getReadResult().getBuildTimeRunTimePatternMap(), ret, javadoc);
        processConfig(config.getReadResult().getBootstrapPatternMap(), ret, javadoc);
        processConfig(config.getReadResult().getRunTimePatternMap(), ret, javadoc);
        return ret;
    }

    private void processConfig(ConfigPatternMap<Container> patterns, List<ConfigDescriptionBuildItem> ret,
            Properties javadoc) {

        patterns.forEach(new Consumer<Container>() {
            @Override
            public void accept(Container node) {
                Field field = node.findField();
                ConfigItem configItem = field.getAnnotation(ConfigItem.class);
                final ConfigProperty configProperty = field.getAnnotation(ConfigProperty.class);
                String defaultDefault;
                final Class<?> valueClass = field.getType();
                if (valueClass == boolean.class) {
                    defaultDefault = "false";
                } else if (valueClass.isPrimitive() && valueClass != char.class) {
                    defaultDefault = "0";
                } else {
                    defaultDefault = null;
                }
                String defVal = defaultDefault;
                if (configItem != null) {
                    final String itemDefVal = configItem.defaultValue();
                    if (!itemDefVal.equals(ConfigItem.NO_DEFAULT)) {
                        defVal = itemDefVal;
                    }
                } else if (configProperty != null) {
                    final String propDefVal = configProperty.defaultValue();
                    if (!propDefVal.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                        defVal = propDefVal;
                    }
                }
                String javadocKey = field.getDeclaringClass().getName().replace('$', '.') + '.' + field.getName();
                ret.add(new ConfigDescriptionBuildItem("quarkus." + node.getPropertyName(),
                        node.findEnclosingClass().getConfigurationClass(),
                        defVal, javadoc.getProperty(javadocKey)));
            }
        });
    }

}

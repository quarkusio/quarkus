package io.quarkus.deployment.steps;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
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

public class ConfigDescriptionBuildStep {

    @BuildStep
    List<ConfigDescriptionBuildItem> createConfigDescriptions(
            ConfigurationBuildItem config) throws Exception {
        Properties javadoc = new Properties();
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
                .getResources("META-INF/quarkus-javadoc.properties");
        while (resources.hasMoreElements()) {
            try (InputStream in = resources.nextElement().openStream()) {
                javadoc.load(in);
            }
        }
        List<ConfigDescriptionBuildItem> ret = new ArrayList<>();
        processConfig(config.getReadResult().getBuildTimePatternMap(), ret, javadoc);
        processConfig(config.getReadResult().getBuildTimeRunTimePatternMap(), ret, javadoc);
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
                String javadocKey = field.getDeclaringClass().getName().replace("$", ".") + "." + field.getName();
                ret.add(new ConfigDescriptionBuildItem("quarkus." + node.getPropertyName(),
                        node.findEnclosingClass().getConfigurationClass(),
                        defVal, javadoc.getProperty(javadocKey)));
            }
        });
    }

}

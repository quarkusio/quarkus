package io.quarkus.deployment.steps;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BuildTimeConfigurationBuildItem;
import io.quarkus.deployment.builditem.BuildTimeRunTimeFixedConfigurationBuildItem;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationBuildItem;
import io.quarkus.deployment.configuration.ConfigDefinition;
import io.quarkus.deployment.configuration.LeafConfigType;

public class ConfigDescriptionBuildStep {

    @BuildStep
    List<ConfigDescriptionBuildItem> createConfigDescriptions(
            RunTimeConfigurationBuildItem runtimeConfig,
            BuildTimeConfigurationBuildItem buildTimeConfig,
            BuildTimeRunTimeFixedConfigurationBuildItem buildTimeRuntimeConfig) throws Exception {
        Properties javadoc = new Properties();
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
                .getResources("META-INF/quarkus-javadoc.properties");
        while (resources.hasMoreElements()) {
            try (InputStream in = resources.nextElement().openStream()) {
                javadoc.load(in);
            }
        }
        List<ConfigDescriptionBuildItem> ret = new ArrayList<>();
        processConfig(runtimeConfig.getConfigDefinition(), ret, javadoc);
        processConfig(buildTimeConfig.getConfigDefinition(), ret, javadoc);
        processConfig(buildTimeRuntimeConfig.getConfigDefinition(), ret, javadoc);
        return ret;
    }

    private void processConfig(ConfigDefinition configDefinition, List<ConfigDescriptionBuildItem> ret, Properties javadoc) {

        configDefinition.getLeafPatterns().forEach(new Consumer<LeafConfigType>() {
            @Override
            public void accept(LeafConfigType leafConfigType) {
                ret.add(new ConfigDescriptionBuildItem("quarkus." + leafConfigType.getConfigKey(),
                        leafConfigType.getItemClass(),
                        leafConfigType.getDefaultValueString(), javadoc.getProperty(leafConfigType.getJavadocKey())));
            }
        });
    }

}

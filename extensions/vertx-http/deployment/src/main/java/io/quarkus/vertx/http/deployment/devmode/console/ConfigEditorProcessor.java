package io.quarkus.vertx.http.deployment.devmode.console;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class ConfigEditorProcessor {

    private static final Logger log = Logger.getLogger(ConfigEditorProcessor.class);

    @BuildStep
    DevConsoleTemplateInfoBuildItem config(List<ConfigDescriptionBuildItem> config) throws Exception {
        List<CurrentConfig> configs = new ArrayList<>();
        Config current = ConfigProvider.getConfig();

        Properties appProperties = new Properties();
        Path appProps = null;
        for (Path i : DevConsoleManager.getHotReplacementContext().getResourcesDir()) {
            Path app = i.resolve("application.properties");
            if (Files.exists(app)) {
                appProps = app;
                break;
            }
        }
        if (appProps != null) {
            try (InputStream in = Files.newInputStream(appProps)) {
                appProperties.load(in);
            }
        }

        for (ConfigDescriptionBuildItem i : config) {
            if (i.getPropertyName().contains("*")) {
                continue; //TODO: complex properties
            }
            configs.add(new CurrentConfig(i.getPropertyName(), i.getDocs(), i.getDefaultValue(),
                    current.getOptionalValue(i.getPropertyName(), String.class).orElse(null),
                    appProperties.getProperty(i.getPropertyName())));
        }
        Collections.sort(configs);

        return new DevConsoleTemplateInfoBuildItem("config", configs);
    }

    @BuildStep
    DevConsoleRouteBuildItem handlePost() {
        return new DevConsoleRouteBuildItem("config", "POST", new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form) throws Exception {
                String key = event.request().getFormAttribute("name");
                String value = event.request().getFormAttribute("value");

                Properties appProperties = new Properties();
                Path appProps = null;
                for (Path i : DevConsoleManager.getHotReplacementContext().getResourcesDir()) {
                    Path app = i.resolve("application.properties");
                    if (Files.exists(app)) {
                        appProps = app;
                        break;
                    }
                }
                boolean present = false;
                if (appProps != null) {
                    try (InputStream in = Files.newInputStream(appProps)) {
                        appProperties.load(in);
                        present = appProperties.containsKey(key);
                    }
                }
                if (!present) {
                    try (OutputStream out = Files.newOutputStream(appProps, StandardOpenOption.APPEND)) {
                        out.write(("\n" + key + "=" + value).getBytes(StandardCharsets.UTF_8)); //TODO: escpaing
                    }
                } else {
                    List<String> lines = Files.readAllLines(appProps);
                    Iterator<String> it = lines.iterator();
                    while (it.hasNext()) {
                        String val = it.next();
                        if (val.startsWith(key + "=")) {
                            it.remove();
                        }
                    }
                    lines.add(key + "=" + value);
                    try (BufferedWriter writer = Files.newBufferedWriter(appProps)) {
                        for (String i : lines) {
                            writer.write(i);
                            writer.newLine();
                        }
                    }
                }
                DevConsoleManager.getHotReplacementContext().doScan(true);
                flashMessage(event, "Configuration updated");
            }
        });
    }

    public static class CurrentConfig implements Comparable<CurrentConfig> {
        private final String propertyName;
        private final String description;
        private final String defaultValue;
        private final String currentValue;
        private final String appPropertiesValue;

        public CurrentConfig(String propertyName, String description, String defaultValue, String currentValue,
                String appPropertiesValue) {
            this.propertyName = propertyName;
            this.description = description;
            this.defaultValue = defaultValue;
            this.currentValue = currentValue;
            this.appPropertiesValue = appPropertiesValue;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getDescription() {
            return description;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getCurrentValue() {
            return currentValue;
        }

        public String getAppPropertiesValue() {
            return appPropertiesValue;
        }

        @Override
        public int compareTo(CurrentConfig o) {
            if (appPropertiesValue == null && o.appPropertiesValue != null) {
                return 1;
            }
            if (appPropertiesValue != null && o.appPropertiesValue == null) {
                return -1;
            }

            return propertyName.compareTo(o.propertyName);
        }
    }

}

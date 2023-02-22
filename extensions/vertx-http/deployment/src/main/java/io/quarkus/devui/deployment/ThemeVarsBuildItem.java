package io.quarkus.devui.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ThemeVarsBuildItem extends SimpleBuildItem {

    private final Set<String> themeVars;
    private final String defaultValue;

    public ThemeVarsBuildItem(Set<String> themeVars, String defaultValue) {
        this.themeVars = themeVars;
        this.defaultValue = defaultValue;
    }

    public Set<String> getThemeVars() {
        return themeVars;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getTemplateValue() {
        try (StringWriter sw = new StringWriter()) {
            for (String line : themeVars) {
                sw.write(line + ": " + defaultValue + ";");
                sw.write("\n");
            }
            return sw.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

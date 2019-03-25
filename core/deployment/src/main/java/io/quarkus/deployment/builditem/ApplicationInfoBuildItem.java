package io.quarkus.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

public final class ApplicationInfoBuildItem extends SimpleBuildItem {

    public static final String UNSET_VALUE = "<<unset>>";

    private final String group;
    private final String name;
    private final String version;
    private final String finalName;
    private final String baseDir;
    private final String wiringClassesDir;

    public ApplicationInfoBuildItem(String group, String name, String version,
            String finalName, String baseDir, String wiringClassesDir) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.finalName = finalName;
        this.baseDir = baseDir;
        this.wiringClassesDir = wiringClassesDir;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getFinalName() {
        return finalName;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public String getWiringClassesDir() {
        return wiringClassesDir;
    }
}

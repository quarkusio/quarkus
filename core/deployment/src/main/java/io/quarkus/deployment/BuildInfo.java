package io.quarkus.deployment;

public class BuildInfo {

    private final String group;
    private final String name;
    private final String version;
    private final String finalName;
    private final String baseDir;
    private final String wiringClassesDir;

    public BuildInfo(String group, String name, String version, String finalName, String baseDir, String wiringClassesDir) {
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

    public static BuildInfo unset() {
        return new BuildInfo("", "", "", "", "", "");
    }
}

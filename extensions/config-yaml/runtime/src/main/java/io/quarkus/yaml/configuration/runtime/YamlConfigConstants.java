package io.quarkus.yaml.configuration.runtime;

public class YamlConfigConstants {

    public static final String APPLICATION_YML_FILE = "application.yaml";
    public static final String MICROPROFILE_CONFIG_YML_FILE = "META-INF/microprofile-config.yaml";

    public static final int MICROPROFILE_CONFIG_YML_JAR_PRIORITY = 251;
    public static final int APPLICATION_YML_JAR_PRIORITY = 252;
    public static final int MICROPROFILE_CONFIG_YML_FILESYSTEM_PRIORITY = 261;
    public static final int APPLICATION_YML_FILESYSTEM_PRIORITY = 262;
}

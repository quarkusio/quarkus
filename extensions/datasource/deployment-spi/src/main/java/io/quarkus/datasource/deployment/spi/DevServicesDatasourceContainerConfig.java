package io.quarkus.datasource.deployment.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class DevServicesDatasourceContainerConfig {

    private final Optional<String> imageName;
    private final Map<String, String> containerEnv;
    private final Map<String, String> containerProperties;
    private final Map<String, String> additionalJdbcUrlProperties;
    private final OptionalInt fixedExposedPort;
    private final Optional<String> command;
    private final Optional<String> dbName;
    private final Optional<String> username;
    private final Optional<String> password;
    private final Optional<List<String>> initScriptPath;
    private final Optional<List<String>> initPrivilegedScriptPath;
    private final Map<String, String> volumes;
    private final boolean reuse;
    private final boolean showLogs;

    public DevServicesDatasourceContainerConfig(Optional<String> imageName,
            Map<String, String> containerEnv,
            Map<String, String> containerProperties,
            Map<String, String> additionalJdbcUrlProperties,
            OptionalInt port,
            Optional<String> command,
            Optional<String> dbName,
            Optional<String> username,
            Optional<String> password,
            Optional<List<String>> initScriptPath,
            Optional<List<String>> initPrivilegedScriptPath,
            Map<String, String> volumes,
            boolean reuse,
            boolean showLogs) {
        this.imageName = imageName;
        this.containerEnv = containerEnv;
        this.containerProperties = containerProperties;
        this.additionalJdbcUrlProperties = additionalJdbcUrlProperties;
        this.fixedExposedPort = port;
        this.command = command;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
        this.initScriptPath = initScriptPath;
        this.initPrivilegedScriptPath = initPrivilegedScriptPath;
        this.volumes = volumes;
        this.reuse = reuse;
        this.showLogs = showLogs;
    }

    public Optional<String> getImageName() {
        return imageName;
    }

    public Map<String, String> getContainerEnv() {
        return containerEnv;
    }

    public Map<String, String> getContainerProperties() {
        return containerProperties;
    }

    public Map<String, String> getAdditionalJdbcUrlProperties() {
        return additionalJdbcUrlProperties;
    }

    public OptionalInt getFixedExposedPort() {
        return fixedExposedPort;
    }

    public Optional<String> getCommand() {
        return command;
    }

    public Optional<String> getDbName() {
        return dbName;
    }

    public Optional<String> getUsername() {
        return username;
    }

    public Optional<String> getPassword() {
        return password;
    }

    public Optional<List<String>> getInitScriptPath() {
        return initScriptPath;
    }

    public Optional<List<String>> getInitPrivilegedScriptPath() {
        return initPrivilegedScriptPath;
    }

    public boolean isShowLogs() {
        return showLogs;
    }

    public Map<String, String> getVolumes() {
        return volumes;
    }

    public boolean isReuse() {
        return reuse;
    }
}

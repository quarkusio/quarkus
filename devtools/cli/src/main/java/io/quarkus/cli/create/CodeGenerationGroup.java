package io.quarkus.cli.create;

import java.util.Collections;
import java.util.Map;

import io.quarkus.devtools.commands.CreateProjectHelper;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.quickcli.annotations.Option;

public class CodeGenerationGroup {
    String packageName;
    Map<String, String> appConfig = Collections.emptyMap();

    @Option(paramLabel = "PACKAGE-NAME", names = {
            "--package-name" }, description = "Base package for generated classes")
    void setPackageName(String name) {
        this.packageName = CreateProjectHelper.checkPackageName(name);
    }

    public String getPackageName() {
        return packageName;
    }

    @Option(names = {
            "--no-wrapper" }, description = "Include a buildtool wrapper (e.g. mvnw, gradlew)", negatable = true)
    public boolean includeWrapper = true;

    @Option(names = {
            "--no-code" }, description = "Include starter code provided by extensions or generate an empty project", negatable = true)
    public boolean includeCode = true;

    @Option(names = {
            "--no-dockerfiles" }, description = "Include standard dockerfiles", negatable = true)
    public boolean includeDockerfiles = true;

    @Option(names = { "-c",
            "--app-config" }, description = "Configuration attributes to be set in the application.properties/yml file. Specify as 'key1=value1,key2=value2'")
    void setAppConfig(String config) {
        if (config.trim().length() > 0) {
            appConfig = ToolsUtils.stringToMap(config, ",", "=");
        }
    }

    public Map<String, String> getAppConfig() {
        return appConfig;
    }

    @Override
    public String toString() {
        return "CodeGenerationGroup ["
                + "includeCode=" + includeCode
                + ", includeWrapper=" + includeWrapper
                + ", includeDockerfiles=" + includeDockerfiles
                + ", packageName=" + packageName
                + "]";
    }
}

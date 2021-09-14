package io.quarkus.cli.create;

import picocli.CommandLine;

public class ExtensionTargetGVGroup {

    String groupId;
    String version;

    @CommandLine.Option(paramLabel = "GROUP-ID", names = { "-g",
            "--group-id" }, description = "The groupId for extension artifacts", defaultValue = "org.acme")
    void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @CommandLine.Option(paramLabel = "VERSION", names = { "-v",
            "--version" }, description = "The initial project version", defaultValue = "1.0.0-SNAPSHOT")
    void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ExtensionTargetGVGroup [groupId=" + groupId + ", version=" + version + "]";
    }
}

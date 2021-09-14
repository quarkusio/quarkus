package io.quarkus.cli.create;

import io.quarkus.devtools.project.codegen.CreateProjectHelper;
import picocli.CommandLine;

public class TargetGAVGroup {
    String groupId;
    String artifactId;
    String version;

    @CommandLine.Option(paramLabel = "GROUP-ID", names = { "-g",
            "--group-id" }, description = "The groupId for maven and gradle artifacts", defaultValue = CreateProjectHelper.DEFAULT_GROUP_ID)
    void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @CommandLine.Option(paramLabel = "ARTIFACT-ID", names = { "-a",
            "--artifact-id" }, description = "The artifactId for maven and gradle artifacts", defaultValue = CreateProjectHelper.DEFAULT_ARTIFACT_ID)
    void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @CommandLine.Option(paramLabel = "VERSION", names = { "-v",
            "--version" }, description = "The initial project version", defaultValue = CreateProjectHelper.DEFAULT_VERSION)
    void setVersion(String version) {
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "TargetGAVGroup [artifactId=" + artifactId + ", groupId=" + groupId + ", version=" + version + "]";
    }
}

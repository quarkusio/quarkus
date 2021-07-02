package io.quarkus.cli.create;

import io.quarkus.devtools.project.codegen.CreateProjectHelper;
import picocli.CommandLine;

public class TargetGAVGroup {
    final static String DEFAULT_GAV = CreateProjectHelper.DEFAULT_GROUP_ID + ":"
            + CreateProjectHelper.DEFAULT_ARTIFACT_ID + ":"
            + CreateProjectHelper.DEFAULT_VERSION;

    String groupId = CreateProjectHelper.DEFAULT_GROUP_ID;
    String artifactId = CreateProjectHelper.DEFAULT_ARTIFACT_ID;
    String version = CreateProjectHelper.DEFAULT_VERSION;

    @CommandLine.Parameters(arity = "0..1", paramLabel = "[GROUP-ID:]ARTIFACT-ID[:VERSION]", description = "Java project identifiers%n"
            + "  default: " + DEFAULT_GAV + "%n"
            + "  Examples:%n"
            + "     my-project%n"
            + "     my.group:my-project%n"
            + "     my.group:my-project:0.1%n")
    String gav = null;

    boolean initialized = false;

    void projectGav() {
        if (!initialized) {
            if (gav != null) {
                // process new gav parameter
                int firstPos = gav.indexOf(":");
                int lastPos = gav.lastIndexOf(":");
                if (firstPos < 0) {
                    // artifact-id  -- use defaults for group id and version (common/demo)
                    artifactId = gav;
                } else {
                    // g::   -- (uncommon)
                    // g::v  -- (uncommon)
                    // g:a   -- COMMON
                    // g:a:  -- (uncommon alternate)
                    // g:a:v -- COMMON
                    // :a:   -- (uncommon alternate)
                    // :a:v  -- (uncommon)
                    if (firstPos != 0) {
                        groupId = gav.substring(0, firstPos);
                    }
                    if (lastPos == firstPos) {
                        artifactId = gav.substring(firstPos + 1);
                    } else if (lastPos >= firstPos + 2) {
                        artifactId = gav.substring(firstPos + 1, lastPos);
                    }
                    if (lastPos > firstPos && lastPos <= gav.length() - 2) {
                        version = gav.substring(lastPos + 1);
                    }
                }
            }
            initialized = true;
        }
    }

    public String getGroupId() {
        projectGav();
        return groupId;
    }

    public String getArtifactId() {
        projectGav();
        return artifactId;
    }

    public String getVersion() {
        projectGav();
        return version;
    }

    @Override
    public String toString() {
        return "TargetGAVGroup [groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + ", gav=" + gav
                + "]";
    }
}

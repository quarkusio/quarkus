package io.quarkus.cli.common;

import io.quarkus.cli.Version;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.registry.catalog.PlatformStreamCoords;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

public class TargetQuarkusVersionGroup {
    final static String FULL_EXAMPLE = ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID + ":"
            + ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID + ":2.2.0.Final";
    PlatformStreamCoords streamCoords = null;
    String validStream = null;

    ArtifactCoords platformBom = null;
    String validPlatformBom = null;

    @CommandLine.Spec
    CommandSpec spec;

    @CommandLine.Option(paramLabel = "platformKey:streamId", names = { "-S",
            "--stream" }, description = "A target stream, for example:%n  io.quarkus.platform:2.0")
    void setStream(String stream) {
        stream = stream.trim();
        if (!stream.isEmpty()) {
            try {
                streamCoords = PlatformStreamCoords.fromString(stream);
                validStream = stream;
            } catch (IllegalArgumentException iex) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                        String.format("Invalid value '%s' for option '--stream'. " +
                                "Value should be specified as 'platformKey:streamId'. %s", iex.getMessage()));
            }
        }
    }

    @CommandLine.Option(paramLabel = "groupId:artifactId:version", names = { "-P",
            "--platform-bom" }, description = "A specific Quarkus platform BOM, for example:%n"
                    + "  " + FULL_EXAMPLE + "%n"
                    + "  io.quarkus::999-SNAPSHOT"
                    + "  2.2.0.Final%n"
                    + "Default groupId: " + ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID + "%n"
                    + "Default artifactId: " + ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID + "%n")
    void setPlatformBom(String bom) {
        bom = bom.replaceFirst("^::", "").trim();
        if (!bom.isEmpty()) {
            try {
                int firstPos = bom.indexOf(":");
                int lastPos = bom.lastIndexOf(":");
                if (lastPos <= 0) {
                    // no : at all, use default group and artifact id
                    setBom(ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID,
                            ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID,
                            bom);
                } else if (lastPos == firstPos + 1) { // We have :: somewhere
                    if (lastPos == bom.length() - 1) {
                        // some.group::, use default artifact and client version
                        setBom(bom.substring(0, firstPos),
                                ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID,
                                Version.clientVersion());
                    } else {
                        // some.group::version, use default artifact id
                        setBom(bom.substring(0, firstPos),
                                ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID,
                                bom.substring(lastPos + 1));
                    }
                } else if (firstPos == 0 && lastPos == bom.length() - 1) {
                    // :my-bom:, use default group and version
                    setBom(ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID,
                            bom.substring(1, lastPos),
                            Version.clientVersion());
                } else {
                    platformBom = ArtifactCoords.fromString(bom);
                    validPlatformBom = bom; // keep original (valid) string (dryrun)
                }
            } catch (IllegalArgumentException iex) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                        String.format("Invalid value '%s' for option '--platform-bom'. " +
                                "Value should be specified as 'GROUP-ID:ARTIFACT-ID:VERSION'. %s", bom, iex.getMessage()));
            }
        }
    }

    public boolean isPlatformSpecified() {
        return platformBom != null;
    }

    public ArtifactCoords getPlatformBom() {
        return platformBom;
    }

    public boolean isStreamSpecified() {
        return streamCoords != null;
    }

    public PlatformStreamCoords getStream() {
        return streamCoords;
    }

    public String dryRun() {
        if (streamCoords != null) {
            return "stream " + validStream;
        } else if (platformBom != null) {
            return "platform " + validPlatformBom;
        } else {
            return "same as project";
        }
    }

    @Override
    public String toString() {
        return "TargetQuarkusVersionGroup{"
                + "stream=" + streamCoords
                + ", platformBom=" + platformBom
                + '}';
    }

    private void setBom(String artifactId, String groupId, String version) {
        platformBom = ArtifactCoords.pom(artifactId, groupId, version);
        validPlatformBom = artifactId + ":" + groupId + ":" + version;
    }
}

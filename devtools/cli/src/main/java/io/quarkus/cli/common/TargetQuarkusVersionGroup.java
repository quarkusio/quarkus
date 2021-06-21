package io.quarkus.cli.common;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.StreamCoords;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

public class TargetQuarkusVersionGroup {
    StreamCoords streamCoords = null;
    String validStream = null;

    ArtifactCoords platformBom = null;
    String validPlatformBom = null;

    @CommandLine.Spec
    CommandSpec spec;

    @CommandLine.Option(paramLabel = "platformKey:streamId", names = { "-S",
            "--stream" }, description = "A target stream, for example:%n  io.quarkus.platform:999-SNAPSHOT%n  io.quarkus:1.13")
    void setStream(String stream) {
        stream = stream.trim();
        if (!stream.isEmpty()) {
            try {
                streamCoords = StreamCoords.fromString(stream);
                validStream = stream;
            } catch (IllegalArgumentException iex) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                        String.format("Invalid value '%s' for option '--stream'. " +
                                "Value should be specified as 'platformKey:streamId'. %s", iex.getMessage()));
            }
        }
    }

    @CommandLine.Option(paramLabel = "groupId:artifactId:version", names = { "-P",
            "--platform-bom" }, description = "A specific Quarkus platform BOM, for example:%n  io.quarkus:quarkus-bom:1.13.4.Final")
    void setPlatformBom(String bom) {
        bom = bom.trim();
        if (!bom.isEmpty()) {
            try {
                platformBom = ArtifactCoords.fromString(bom);
                validPlatformBom = bom; // keep original (valid) string (dryrun)
            } catch (IllegalArgumentException iex) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                        String.format("Invalid value '%s' for option '--platform-bom'. " +
                                "Value should be specified as 'groupId:artifactId:version'. %s", bom, iex.getMessage()));
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

    public StreamCoords getStream() {
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
}

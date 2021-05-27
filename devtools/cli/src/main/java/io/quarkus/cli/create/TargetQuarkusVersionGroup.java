package io.quarkus.cli.create;

import io.quarkus.maven.ArtifactCoords;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

public class TargetQuarkusVersionGroup {
    ArtifactCoords platformBom = null;

    @CommandLine.Spec
    CommandSpec spec;

    @CommandLine.Option(paramLabel = "STREAM", names = { "-S",
            "--stream" }, description = "A target stream, e.g. default, snapshot", defaultValue = "default", hidden = true)
    String stream;

    @CommandLine.Option(paramLabel = "groupId:artifactId:version", names = { "-p",
            "--platform-bom" }, description = "A specific Quarkus platform BOM,%ne.g. io.quarkus:quarkus-bom:1.13.4.Final")
    void setPlatformBom(String bom) {
        bom = bom.trim();
        if (!bom.isEmpty()) {
            try {
                platformBom = ArtifactCoords.fromString(bom);
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

    public boolean isStream() {
        return platformBom == null;
    }

    public String getStream() {
        return stream;
    }

    @Override
    public String toString() {
        return "TargetQuarkusVersionGroup{"
                + "stream=" + stream
                + ", platformBom=" + platformBom
                + '}';
    }
}

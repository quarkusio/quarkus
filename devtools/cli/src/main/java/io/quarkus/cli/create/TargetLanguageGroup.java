package io.quarkus.cli.create;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.JavaVersion;
import io.quarkus.devtools.project.SourceType;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

public class TargetLanguageGroup {
    SourceType sourceType;

    static class VersionCandidates extends ArrayList<String> {
        VersionCandidates() {
            super(JavaVersion.JAVA_VERSIONS_LTS.stream().map(String::valueOf).collect(Collectors.toList()));
        }
    }

    @CommandLine.Option(names = {
            "--java" }, description = "Target Java version.\n  Valid values: ${COMPLETION-CANDIDATES}", completionCandidates = VersionCandidates.class, defaultValue = JavaVersion.DETECT_JAVA_RUNTIME_VERSION)
    String javaVersion = JavaVersion.DETECT_JAVA_RUNTIME_VERSION;

    @CommandLine.Option(names = { "--kotlin" }, description = "Use Kotlin")
    boolean kotlin = false;

    @CommandLine.Option(names = { "--scala" }, description = "Use Scala")
    boolean scala = false;

    public SourceType getSourceType(CommandSpec spec, BuildTool buildTool, Set<String> extensions, OutputOptionMixin output) {
        if (kotlin && scala) {
            throw new ParameterException(spec.commandLine(),
                    "Invalid source type. Projects can target either Kotlin (--kotlin) or Scala (--scala), not both.");
        }

        if (sourceType == null) {
            if (buildTool == null) {
                // Buildless/JBang only works with Java, atm
                sourceType = SourceType.JAVA;
                if (kotlin || scala) {
                    output.warn("JBang only supports Java. Using Java as the target language.");
                }
            } else if (kotlin) {
                sourceType = SourceType.KOTLIN;
            } else if (scala) {
                sourceType = SourceType.SCALA;
            } else {
                sourceType = SourceType.resolve(extensions);
            }
        }
        return sourceType;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    @Override
    public String toString() {
        return "TargetLanguageGroup [java=" + javaVersion
                + ", kotlin=" + kotlin
                + ", scala=" + scala
                + ", sourceType=" + sourceType
                + "]";
    }
}

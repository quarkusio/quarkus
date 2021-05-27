package io.quarkus.cli.create;

import java.util.Set;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.codegen.CreateProjectHelper;
import io.quarkus.devtools.project.codegen.SourceType;
import picocli.CommandLine;

public class TargetLanguageGroup {
    SourceType sourceType;

    @CommandLine.Option(names = { "--java" }, description = "Use Java")
    boolean java = false;

    @CommandLine.Option(names = { "--kotlin" }, description = "Use Kotlin")
    boolean kotlin = false;

    @CommandLine.Option(names = { "--scala" }, description = "Use Scala")
    boolean scala = false;

    public SourceType getSourceType(BuildTool buildTool, Set<String> extensions, OutputOptionMixin output) {
        if (sourceType == null) {
            if (buildTool == null) {
                // Buildless/JBang only works with Java, atm
                sourceType = SourceType.JAVA;
                if (kotlin || scala) {
                    output.warn("JBang only supports Java. Using Java as the target language.");
                }
            } else if (kotlin || BuildTool.GRADLE_KOTLIN_DSL == buildTool) {
                sourceType = SourceType.KOTLIN;
            } else if (scala) {
                sourceType = SourceType.SCALA;
            } else {
                sourceType = CreateProjectHelper.determineSourceType(extensions);
            }
        }
        return sourceType;
    }

    @Override
    public String toString() {
        return "TargetLanguageGroup [java=" + java + ", kotlin=" + kotlin + ", scala=" + scala + ", sourceType=" + sourceType
                + "]";
    }
}

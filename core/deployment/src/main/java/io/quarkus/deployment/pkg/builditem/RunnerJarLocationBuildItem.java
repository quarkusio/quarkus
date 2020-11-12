package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class RunnerJarLocationBuildItem extends SimpleBuildItem {

    private final String classifier;
    private final String fullName;
    private final Path fullPath;

    public RunnerJarLocationBuildItem(Path outputDirectory, String baseName, String runnerSuffix) {
        this.classifier = suffixToClassifier(runnerSuffix);
        this.fullName = baseName + runnerSuffix + ".jar";
        this.fullPath = outputDirectory.resolve(fullName);
    }

    public String getFullName() {
        return fullName;
    }

    public Path getFullPath() {
        return fullPath;
    }

    public String getClassifier() {
        return classifier;
    }

    private String suffixToClassifier(String runnerSuffix) {
        if (runnerSuffix == null || runnerSuffix.isEmpty()) {
            return null;
        }

        return runnerSuffix.startsWith("-") ? runnerSuffix.substring(1) : runnerSuffix;
    }
}

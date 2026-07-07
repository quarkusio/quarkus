package io.quarkus.gradle.application.internal.planning;

import org.gradle.api.GradleException;

public final class PackageOutputName {

    private PackageOutputName() {
    }

    public static String assemble(String baseName, String baseNameSuffix, String version) {
        if (baseName == null || baseName.isBlank()) {
            throw new GradleException("Quarkus application archiveBaseName must not be blank.");
        }
        if ("unspecified".equals(version)) {
            throw new GradleException("Quarkus application archiveVersion defaults to project.version, "
                    + "but project.version is unspecified. Configure project.version, archiveVersion, or outputName.");
        }
        StringBuilder name = new StringBuilder(baseName);
        if (baseNameSuffix != null && !baseNameSuffix.isBlank()) {
            name.append(baseNameSuffix);
        }
        if (version != null && !version.isBlank()) {
            name.append('-').append(version);
        }
        return name.toString();
    }
}

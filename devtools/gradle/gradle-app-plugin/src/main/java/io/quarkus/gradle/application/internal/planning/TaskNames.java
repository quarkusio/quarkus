package io.quarkus.gradle.application.internal.planning;

public record TaskNames(String build, String showEffectiveConfig, String run, String imageBuild, String imagePush,
        String startupArchiveValidation, String startupOptimizedImageBuild, String startupOptimizedImagePush,
        String nativeTest) {
}

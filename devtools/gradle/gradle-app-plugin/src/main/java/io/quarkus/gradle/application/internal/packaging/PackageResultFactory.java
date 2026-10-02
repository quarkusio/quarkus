package io.quarkus.gradle.application.internal.packaging;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.GradleException;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.JarResult;
import io.quarkus.gradle.application.internal.execution.BuildRequest;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

public final class PackageResultFactory {

    public PackageResult fromAugmentResult(BuildRequest request, AugmentResult result) {
        if (result == null || result.getJar() == null) {
            throw new GradleException("Quarkus package build for '" + request.descriptor().name()
                    + "' did not produce a jar result");
        }
        JarResult jar = result.getJar();
        if (jar.getPath() == null) {
            throw new GradleException("Quarkus package build for '" + request.descriptor().name()
                    + "' did not report a primary jar path");
        }
        validateShape(request, jar);
        return new PackageResult(
                request.descriptor().name(),
                request.descriptor().type(),
                request.outputRoot(),
                request.buildSystemProperties().getOrDefault("quarkus.package.output-name",
                        request.descriptor().name()),
                jar.getPath(),
                Optional.ofNullable(jar.getOriginalArtifact()),
                Optional.ofNullable(jar.getLibraryDir()),
                jar.mutable(),
                jar.isUberJar(),
                Optional.ofNullable(jar.getClassifier()).filter(value -> !value.isBlank()),
                artifacts(result.getResults()));
    }

    private static void validateShape(BuildRequest request, JarResult jar) {
        QuarkusApplicationBuildType type = request.descriptor().type();
        if (!type.isJar()) {
            throw new GradleException("Quarkus package result requested for non-JVM output '"
                    + request.descriptor().name() + "' of type " + type);
        }
        if (type == QuarkusApplicationBuildType.MUTABLE_JAR) {
            if (!jar.mutable()) {
                throw new GradleException("Quarkus package build for '" + request.descriptor().name()
                        + "' expected a mutable-jar result but Quarkus reported a non-mutable jar");
            }
            return;
        }
        if (jar.mutable()) {
            throw new GradleException("Quarkus package build for '" + request.descriptor().name()
                    + "' expected " + type.jarType().orElse(type.name()) + " but Quarkus reported a mutable jar");
        }
        if (type == QuarkusApplicationBuildType.UBER_JAR) {
            if (!jar.isUberJar()) {
                throw new GradleException("Quarkus package build for '" + request.descriptor().name()
                        + "' expected an uber-jar result but Quarkus reported a library directory");
            }
            return;
        }
        if (jar.isUberJar()) {
            throw new GradleException("Quarkus package build for '" + request.descriptor().name()
                    + "' expected " + type.jarType().orElse(type.name()) + " but Quarkus reported an uber jar");
        }
    }

    private static List<PackageResult.Artifact> artifacts(List<ArtifactResult> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .filter(result -> result.getType() != null && !result.getType().isBlank())
                .map(result -> new PackageResult.Artifact(
                        Optional.ofNullable(result.getPath()),
                        result.getType(),
                        result.getMetadata() == null ? Map.of() : result.getMetadata()))
                .toList();
    }
}

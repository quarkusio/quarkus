package io.quarkus.gradle.application.internal.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.provider.Provider;

import io.quarkus.gradle.application.dsl.QuarkusApplicationBuild;

final class QuarkusApplicationIntegrationTestBuildReference {

    private final String displayName;
    private final Provider<String> buildNameProvider;

    private QuarkusApplicationIntegrationTestBuildReference(String displayName, Provider<String> buildNameProvider) {
        this.displayName = displayName;
        this.buildNameProvider = buildNameProvider;
    }

    static QuarkusApplicationIntegrationTestBuildReference of(Object notation) {
        if (notation instanceof CharSequence name) {
            return named(name.toString());
        }
        if (notation instanceof NamedDomainObjectProvider<?> provider) {
            return named(provider.getName());
        }
        if (notation instanceof Provider<?> provider) {
            return new QuarkusApplicationIntegrationTestBuildReference(provider.toString(),
                    provider.map(QuarkusApplicationIntegrationTestBuildReference::buildName));
        }
        if (notation instanceof QuarkusApplicationBuild build) {
            return named(build.getName());
        }
        throw new GradleException("forQuarkusIntegrationTests(...) accepts a build name, "
                + "Provider<? extends QuarkusApplicationBuild>, or QuarkusApplicationBuild, got "
                + (notation == null ? "null" : notation.getClass().getName()));
    }

    private static QuarkusApplicationIntegrationTestBuildReference named(String buildName) {
        if (buildName == null || buildName.isBlank()) {
            throw new GradleException("forQuarkusIntegrationTests(...) requires a non-empty Quarkus application build name");
        }
        return new QuarkusApplicationIntegrationTestBuildReference(buildName, null);
    }

    private static String buildName(Object value) {
        if (value instanceof QuarkusApplicationBuild build) {
            return build.getName();
        }
        if (value instanceof CharSequence name) {
            return name.toString();
        }
        throw new GradleException("forQuarkusIntegrationTests(...) provider must produce a "
                + "QuarkusApplicationBuild or build name, got "
                + (value == null ? "null" : value.getClass().getName()));
    }

    String displayName() {
        return displayName;
    }

    String buildName() {
        if (buildNameProvider == null) {
            return displayName;
        }
        String buildName = buildNameProvider.get();
        if (buildName == null || buildName.isBlank()) {
            throw new GradleException("forQuarkusIntegrationTests(...) resolved to an empty Quarkus application build name");
        }
        return buildName;
    }
}

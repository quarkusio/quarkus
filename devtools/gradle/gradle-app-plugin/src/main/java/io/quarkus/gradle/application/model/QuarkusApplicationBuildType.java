package io.quarkus.gradle.application.model;

import java.util.Optional;

/**
 * Package kinds supported by a named application build.
 */
public enum QuarkusApplicationBuildType {
    /**
     * Produces the default fast-JAR directory layout.
     */
    FAST_JAR(false, false, true, Optional.of("fast-jar")),
    /**
     * Quarkus retains the historical AOT-JAR package name for the layout that
     * supports all JVM startup archive types, including AOT, SCC, and AppCDS.
     */
    AOT_JAR(false, false, true, Optional.of("aot-jar")),
    /**
     * Produces the legacy-JAR directory layout.
     */
    LEGACY_JAR(false, false, true, Optional.of("legacy-jar")),
    /**
     * Produces the mutable-JAR directory layout used by remote development.
     */
    MUTABLE_JAR(false, false, false, Optional.of("mutable-jar")),
    /**
     * Produces one executable uber JAR.
     */
    UBER_JAR(false, false, false, Optional.of("uber-jar")),
    /**
     * Produces a native executable.
     */
    NATIVE_EXECUTABLE(true, false, true, Optional.empty()),
    /**
     * Produces the sources and configuration needed for a later native-image
     * build without creating the executable.
     */
    NATIVE_SOURCES(true, true, false, Optional.empty());

    private final boolean nativeOutput;
    private final boolean nativeSources;
    private final boolean reusableDependencyFragmentCandidate;
    private final Optional<String> jarType;

    QuarkusApplicationBuildType(boolean nativeOutput, boolean nativeSources, boolean reusableDependencyFragmentCandidate,
            Optional<String> jarType) {
        this.nativeOutput = nativeOutput;
        this.nativeSources = nativeSources;
        this.reusableDependencyFragmentCandidate = reusableDependencyFragmentCandidate;
        this.jarType = jarType;
    }

    /**
     * Returns whether this type maps to a Quarkus JAR package type.
     *
     * @return {@code true} for JAR package layouts
     */
    public boolean isJar() {
        return jarType.isPresent();
    }

    /**
     * Returns whether this type produces native output rather than a JAR
     * package.
     *
     * @return {@code true} for native executable or native-sources output
     */
    public boolean isNativeOutput() {
        return nativeOutput;
    }

    /**
     * Returns whether this type produces native-image sources only.
     *
     * @return {@code true} only for {@link #NATIVE_SOURCES}
     */
    public boolean isNativeSources() {
        return nativeSources;
    }

    /**
     * Returns whether integration-test preparation may reuse the package
     * dependency fragment for this output shape.
     *
     * @return {@code true} when the output can reuse that fragment
     */
    public boolean canReuseDependencyFragment() {
        return reusableDependencyFragmentCandidate;
    }

    /**
     * Returns the Quarkus {@code quarkus.package.jar.type} value.
     *
     * @return the JAR type, or empty for native outputs
     */
    public Optional<String> jarType() {
        return jarType;
    }
}

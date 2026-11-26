package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactKey;

class JarTreeShakeKeptClassRootsTest {

    private static final ArtifactKey SHAKEN = ArtifactKey.fromString("org.acme:shaken");
    private static final ArtifactKey EXCLUDED = ArtifactKey.fromString("org.acme:excluded");

    private static final Map<String, Supplier<byte[]>> DEP_BYTECODE = Map.of(
            "lib.Dep", () -> new byte[0],
            "lib.Outer$Inner", () -> new byte[0],
            "excl.Kept", () -> new byte[0]);
    private static final Map<String, ArtifactKey> CLASS_TO_DEP = Map.of(
            "lib.Dep", SHAKEN,
            "lib.Outer$Inner", SHAKEN,
            "excl.Kept", EXCLUDED);

    private static final Predicate<String> REACHABILITY_ANALYZED = JarTreeShakeInput.reachabilityAnalyzed(
            DEP_BYTECODE, CLASS_TO_DEP, Set.of(EXCLUDED));
    private static final Predicate<String> ALWAYS_PRESENT = JarTreeShakeInput.alwaysPresent(
            DEP_BYTECODE, Map.of("app.App", () -> new byte[0]), Map.of());

    @Test
    void shakenDependencyClassesBecomeRoots() {
        Set<String> roots = new HashSet<>();

        JarTreeShakeInput.addKeptClassRoots(Set.of("lib.Dep", "lib.Outer$Inner"), REACHABILITY_ANALYZED,
                ALWAYS_PRESENT, roots);

        assertThat(roots).containsExactlyInAnyOrder("lib.Dep", "lib.Outer$Inner");
    }

    @Test
    void classesThatAreNeverRemovedAreNotRoots() {
        Set<String> roots = new HashSet<>();

        JarTreeShakeInput.addKeptClassRoots(Set.of("excl.Kept", "app.App", "java.lang.Thread"),
                REACHABILITY_ANALYZED, ALWAYS_PRESENT, roots);

        assertThat(roots).isEmpty();
    }

    @Test
    void unknownClassesAreNotRoots() {
        Set<String> roots = new HashSet<>();

        // a canonical name of a member class doesn't match its binary name
        JarTreeShakeInput.addKeptClassRoots(Set.of("lib.Outer.Inner", "org.absent.Missing"),
                REACHABILITY_ANALYZED, ALWAYS_PRESENT, roots);

        assertThat(roots).isEmpty();
    }
}

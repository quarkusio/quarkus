package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.pkg.builditem.JarTreeShakeConditionalRootClassBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

class JarTreeShakeConditionalRootsTest {

    private static final ArtifactKey SHAKEN = ArtifactKey.fromString("org.acme:shaken");
    private static final ArtifactKey EXCLUDED = ArtifactKey.fromString("org.acme:excluded");

    private static final Map<String, Supplier<byte[]>> DEP_BYTECODE = Map.of(
            "lib.Dep", () -> new byte[0],
            "excl.Kept", () -> new byte[0]);
    private static final Map<String, Supplier<byte[]>> APP_BYTECODE = Map.of("app.App", () -> new byte[0]);
    private static final Map<String, Supplier<byte[]>> GENERATED_BYTECODE = Map.of("gen.Generated", () -> new byte[0]);
    private static final Map<String, ArtifactKey> CLASS_TO_DEP = Map.of("lib.Dep", SHAKEN, "excl.Kept", EXCLUDED);

    @Test
    void reachabilityIsAnalyzedOnlyForShakenDependencyClasses() {
        Predicate<String> analyzed = JarTreeShakeInput.reachabilityAnalyzed(DEP_BYTECODE, CLASS_TO_DEP, Set.of(EXCLUDED));

        assertThat(analyzed).accepts("lib.Dep");
        assertThat(analyzed).rejects("excl.Kept", "app.App", "gen.Generated", "java.lang.Thread", "org.absent.Missing");
    }

    @Test
    void applicationGeneratedExcludedAndJdkClassesAreAlwaysPresent() {
        Predicate<String> alwaysPresent = JarTreeShakeInput.alwaysPresent(DEP_BYTECODE, APP_BYTECODE, GENERATED_BYTECODE);

        assertThat(alwaysPresent).accepts("excl.Kept", "app.App", "gen.Generated", "java.lang.Thread");
        // GraalVM builder classes and JDK classes the building JVM may not have (other platforms, newer JDKs)
        assertThat(alwaysPresent).accepts("com.oracle.svm.core.jdk.SystemPropertiesSupport",
                "sun.awt.windows.WToolkit", "jdk.internal.NotInThisJdk");
        assertThat(alwaysPresent).rejects("org.absent.Missing");
    }

    @Test
    void analyzedConditionIsConditional() {
        Set<String> roots = new HashSet<>();
        Map<String, Set<String>> conditionalRoots = new HashMap<>();

        JarTreeShakeInput.addConditionalRoots(
                List.of(new JarTreeShakeConditionalRootClassBuildItem("lib.Target", "lib.Subst")),
                Set.of("lib.Target")::contains, Set.<String> of()::contains, roots, conditionalRoots);

        assertThat(roots).isEmpty();
        assertThat(conditionalRoots).isEqualTo(Map.of("lib.Target", Set.of("lib.Subst")));
    }

    @Test
    void alwaysPresentConditionIsRoot() {
        Set<String> roots = new HashSet<>();
        Map<String, Set<String>> conditionalRoots = new HashMap<>();

        JarTreeShakeInput.addConditionalRoots(
                List.of(new JarTreeShakeConditionalRootClassBuildItem("java.lang.Thread", "lib.SubstThread")),
                Set.<String> of()::contains, Set.of("java.lang.Thread")::contains, roots, conditionalRoots);

        assertThat(roots).containsExactly("lib.SubstThread");
        assertThat(conditionalRoots).isEmpty();
    }

    @Test
    void absentConditionIsIgnored() {
        Set<String> roots = new HashSet<>();
        Map<String, Set<String>> conditionalRoots = new HashMap<>();

        JarTreeShakeInput.addConditionalRoots(
                List.of(new JarTreeShakeConditionalRootClassBuildItem("org.absent.Missing", "lib.SubstMissing")),
                Set.<String> of()::contains, Set.<String> of()::contains, roots, conditionalRoots);

        assertThat(roots).isEmpty();
        assertThat(conditionalRoots).isEmpty();
    }
}

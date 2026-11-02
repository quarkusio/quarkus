package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.TreeShakeInputFixture.classAnnotatedWith;
import static io.quarkus.deployment.pkg.steps.TreeShakeInputFixture.classExtending;
import static io.quarkus.deployment.pkg.steps.TreeShakeInputFixture.classImplementing;
import static io.quarkus.deployment.pkg.steps.TreeShakeInputFixture.classReferencing;
import static io.quarkus.deployment.pkg.steps.TreeShakeInputFixture.classWithMembers;
import static io.quarkus.deployment.pkg.steps.TreeShakeInputFixture.keptDependencyClasses;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JarTreeShakerReachabilityTest {

    private static final String LIB = "org.acme:lib";

    @Test
    void unreferencedDependencyClassIsRemoved() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .application(classReferencing("app.App", "lib.Used"))
                .dependency(LIB, classReferencing("lib.Used"), classReferencing("lib.Unused"))
                .build();

        assertThat(keptDependencyClasses(input)).containsExactly("lib.Used");
    }

    @Test
    void transitiveReferencesAreKept() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .application(classReferencing("app.App", "lib.A"))
                .dependency(LIB,
                        classReferencing("lib.A", "lib.B"),
                        classReferencing("lib.B", "lib.C"),
                        classReferencing("lib.C"),
                        classReferencing("lib.D"))
                .build();

        assertThat(keptDependencyClasses(input)).containsExactlyInAnyOrder("lib.A", "lib.B", "lib.C");
    }

    @Test
    void conditionalRootsReachFixedPoint() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .application(classReferencing("app.App", "lib.Cond1"))
                .dependency(LIB,
                        classReferencing("lib.Cond1"),
                        classReferencing("lib.T1", "lib.Cond2"),
                        classReferencing("lib.Cond2"),
                        classReferencing("lib.T2"),
                        classReferencing("lib.T3"))
                .conditionalRoot("lib.Cond1", "lib.T1")
                .conditionalRoot("lib.Cond2", "lib.T2")
                .conditionalRoot("lib.Never", "lib.T3")
                .build();

        assertThat(keptDependencyClasses(input))
                .containsExactlyInAnyOrder("lib.Cond1", "lib.T1", "lib.Cond2", "lib.T2");
    }

    @Test
    void substitutionWithReachableTargetIsKept() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .application(classReferencing("app.App", "lib.Target"))
                .dependency(LIB,
                        classReferencing("lib.Target"),
                        classReferencing("lib.Subst", "lib.Helper"),
                        classReferencing("lib.Helper"),
                        classReferencing("lib.Other"))
                .conditionalRoot("lib.Target", "lib.Subst")
                .build();

        assertThat(keptDependencyClasses(input)).containsExactlyInAnyOrder("lib.Target", "lib.Subst", "lib.Helper");
    }

    @Test
    void substitutionWithUnreachableTargetIsRemoved() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .application(classReferencing("app.App"))
                .dependency(LIB,
                        classReferencing("lib.Target"),
                        classReferencing("lib.Subst", "lib.Helper"),
                        classReferencing("lib.Helper"))
                .conditionalRoot("lib.Target", "lib.Subst")
                .build();

        assertThat(keptDependencyClasses(input)).isEmpty();
    }

    @Test
    void referencedSubstitutionKeepsItsTarget() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .application(classReferencing("app.App", "lib.Subst"))
                .dependency(LIB, classReferencing("lib.Target"), classReferencing("lib.Subst"))
                .conditionalRoot("lib.Target", "lib.Subst")
                .conditionalRoot("lib.Subst", "lib.Target")
                .build();

        assertThat(keptDependencyClasses(input)).containsExactlyInAnyOrder("lib.Target", "lib.Subst");
    }

    @Test
    void conditionsAreReevaluatedAfterHigherVersionReferences() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .application(classReferencing("app.App", "lib.A"))
                .dependency(LIB,
                        classReferencing("lib.A"),
                        classReferencing("lib.Target"),
                        classReferencing("lib.Subst"))
                .higherVersion(classReferencing("lib.A", "lib.Target"))
                .conditionalRoot("lib.Target", "lib.Subst")
                .build();

        assertThat(keptDependencyClasses(input)).containsExactlyInAnyOrder("lib.A", "lib.Target", "lib.Subst");
    }

    @Test
    void conditionChainThroughHigherVersionReferences() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .application(classReferencing("app.App", "lib.A"))
                .dependency(LIB,
                        classReferencing("lib.A"),
                        classReferencing("lib.T1"),
                        classReferencing("lib.S1"),
                        classReferencing("lib.T2"),
                        classReferencing("lib.S2"),
                        classReferencing("lib.Other"))
                .higherVersion(classReferencing("lib.A", "lib.T1"))
                .higherVersion(classReferencing("lib.S1", "lib.T2"))
                .conditionalRoot("lib.T1", "lib.S1")
                .conditionalRoot("lib.T2", "lib.S2")
                .build();

        assertThat(keptDependencyClasses(input))
                .containsExactlyInAnyOrder("lib.A", "lib.T1", "lib.S1", "lib.T2", "lib.S2");
    }

    @Test
    void declaredClassesOfInnerClassSubstitutionHolderAreLoadable() {
        JarTreeShakeInput input = innerClassSubstitutionInput(classReferencing("app.App", "lib.Outer$Used"));

        // native-image calls Outer.getDeclaredClasses(), which loads Outer$Unused and its superclass,
        // but nothing runs Outer$Unused's code
        assertThat(keptDependencyClasses(input)).containsExactlyInAnyOrder(
                "lib.Outer$Used", "lib.Subst", "lib.Outer", "lib.Outer$Unused", "lib.Base", "lib.BaseInterface");
    }

    @Test
    void declaredClassesAreNotKeptWithoutTheSubstitution() {
        JarTreeShakeInput input = innerClassSubstitutionInput(classReferencing("app.App"));

        assertThat(keptDependencyClasses(input)).isEmpty();
    }

    @Test
    void loadableClassReachedThroughCodeIsTraced() {
        JarTreeShakeInput input = innerClassSubstitutionInput(
                classReferencing("app.App", "lib.Outer$Used", "lib.Outer$Unused"));

        assertThat(keptDependencyClasses(input)).containsExactlyInAnyOrder(
                "lib.Outer$Used", "lib.Subst", "lib.Outer", "lib.Outer$Unused", "lib.Base", "lib.BaseInterface",
                "lib.Helper");
    }

    /**
     * {@code lib.Subst} substitutes {@code lib.Outer$Used}, given as {@code className = "lib.Outer", innerClass = "Used"}.
     * {@code lib.Outer} also declares {@code lib.Outer$Unused}, which extends {@code lib.Base} (implementing
     * {@code lib.BaseInterface}) and calls {@code lib.Helper}.
     */
    private static JarTreeShakeInput innerClassSubstitutionInput(byte[] app) {
        return TreeShakeInputFixture.builder()
                .application(app)
                .dependency(LIB,
                        classWithMembers("lib.Outer", "lib.Outer$Used", "lib.Outer$Unused"),
                        classReferencing("lib.Outer$Used"),
                        classExtending("lib.Outer$Unused", "lib.Base", "lib.Helper"),
                        classImplementing("lib.Base", "lib.BaseInterface"),
                        classReferencing("lib.BaseInterface"),
                        classReferencing("lib.Helper"),
                        classReferencing("lib.Subst"),
                        classReferencing("lib.Other"))
                .conditionalRoot("lib.Outer$Used", "lib.Subst")
                .conditionalRoot("lib.Subst", "lib.Outer$Used")
                .declaredClasses("lib.Subst", "lib.Outer")
                .build();
    }

    @Test
    void rootBytecodeReferencesAreKept() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .rootBytecode(classReferencing("predef.Foo", "lib.Base"))
                .dependency(LIB, classReferencing("lib.Base"), classReferencing("lib.Other"))
                .build();

        assertThat(keptDependencyClasses(input)).containsExactly("lib.Base");
    }

    @Test
    void annotationClassLiteralIsTraced() {
        JarTreeShakeInput input = TreeShakeInputFixture.builder()
                .dependency(LIB,
                        classAnnotatedWith("lib.Annotated", "lib.Anno", "lib.Target"),
                        classReferencing("lib.Anno"),
                        classReferencing("lib.Target"),
                        classReferencing("lib.Other"))
                .root("lib.Annotated")
                .build();

        assertThat(keptDependencyClasses(input)).containsExactlyInAnyOrder("lib.Annotated", "lib.Anno", "lib.Target");
    }
}

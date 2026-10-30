package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.TreeShakeInputFixture.classAnnotatedWith;
import static io.quarkus.deployment.pkg.steps.TreeShakeInputFixture.classReferencing;
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

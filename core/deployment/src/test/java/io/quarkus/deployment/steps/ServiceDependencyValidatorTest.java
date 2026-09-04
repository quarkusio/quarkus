package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.core.deployment.action.impl.Dependency;
import io.quarkus.core.deployment.action.impl.ServiceMetadataBuildItem;
import io.quarkus.core.deployment.action.impl.StaticServiceMetadataBuildItem;
import io.quarkus.deployment.Phase;

/**
 * Tests for {@link ServiceDependencyValidator}.
 */
class ServiceDependencyValidatorTest {

    /** Convenience: create a runtime service metadata item with default APPLICATION phase. */
    private static ServiceMetadataBuildItem rt(Class<?> type, List<String> name, List<Dependency> deps, String step) {
        return new ServiceMetadataBuildItem(type, name, deps, step, Phase.APPLICATION);
    }

    // ── Existing runtime-only tests (adapted to two-arg signature) ──

    @Test
    void emptyListPassesValidation() {
        ServiceDependencyValidator.validate(List.of(), List.of());
    }

    @Test
    void singleServiceWithNoDependenciesPasses() {
        ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of(), List.of(), "TestStep#test")));
    }

    @Test
    void satisfiedDependencyPasses() {
        ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of(), List.of(), "StepA#a"),
                rt(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of(), 0)),
                        "StepB#b")));
    }

    @Test
    void namedServicesSatisfyNamedDependencies() {
        ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of("primary"), List.of(), "StepA#a"),
                rt(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of("primary"), 0)),
                        "StepB#b")));
    }

    @Test
    void duplicateServiceDetected() {
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of(), List.of(), "StepA#a"),
                rt(String.class, List.of(), List.of(), "StepB#b"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate")
                .hasMessageContaining("java.lang.String:")
                .hasMessageContaining("StepA#a")
                .hasMessageContaining("StepB#b");
    }

    @Test
    void duplicateNamedServiceDetected() {
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of("foo"), List.of(), "StepA#a"),
                rt(String.class, List.of("foo"), List.of(), "StepB#b"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate")
                .hasMessageContaining("java.lang.String:foo");
    }

    @Test
    void differentNamesAreNotDuplicates() {
        ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of("foo"), List.of(), "StepA#a"),
                rt(String.class, List.of("bar"), List.of(), "StepB#b")));
    }

    @Test
    void unsatisfiedDependencyDetected() {
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(List.of(), List.of(
                rt(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of(), 0)),
                        "StepA#a"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsatisfied dependency")
                .hasMessageContaining("java.lang.String:")
                .hasMessageContaining("StepA#a");
    }

    @Test
    void unsatisfiedNamedDependencyDetected() {
        // provide unnamed String, but depend on named "foo" String
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of(), List.of(), "StepA#a"),
                rt(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of("foo"), 0)),
                        "StepB#b"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsatisfied dependency")
                .hasMessageContaining("java.lang.String:foo");
    }

    @Test
    void directCycleDetected() {
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of(),
                        List.of(new Dependency(Integer.class, List.of(), 0)),
                        "StepA#a"),
                rt(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of(), 0)),
                        "StepB#b"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void transitiveCycleDetected() {
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of(),
                        List.of(new Dependency(Long.class, List.of(), 0)),
                        "StepA#a"),
                rt(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of(), 0)),
                        "StepB#b"),
                rt(Long.class, List.of(),
                        List.of(new Dependency(Integer.class, List.of(), 0)),
                        "StepC#c"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void diamondDependencyIsNotACycle() {
        // A depends on B and C; both B and C depend on D — no cycle
        ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of("D"), List.of(), "StepD#d"),
                rt(String.class, List.of("B"),
                        List.of(new Dependency(String.class, List.of("D"), 0)),
                        "StepB#b"),
                rt(String.class, List.of("C"),
                        List.of(new Dependency(String.class, List.of("D"), 0)),
                        "StepC#c"),
                rt(String.class, List.of("A"),
                        List.of(new Dependency(String.class, List.of("B"), 0),
                                new Dependency(String.class, List.of("C"), 0)),
                        "StepA#a")));
    }

    @Test
    void multipleErrorsReportedTogether() {
        // duplicate AND unsatisfied dependency
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(List.of(), List.of(
                rt(String.class, List.of(), List.of(), "StepA#a"),
                rt(String.class, List.of(), List.of(), "StepB#b"),
                rt(Integer.class, List.of(),
                        List.of(new Dependency(Long.class, List.of(), 0)),
                        "StepC#c"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate")
                .hasMessageContaining("Unsatisfied dependency");
    }

    @Test
    void serviceKeyFormat() {
        assertThat(rt(String.class, List.of(), List.of(), "s").serviceKey())
                .isEqualTo("java.lang.String:");
        assertThat(rt(String.class, List.of("foo"), List.of(), "s").serviceKey())
                .isEqualTo("java.lang.String:foo");
    }

    @Test
    void dependencyKeyFormat() {
        assertThat(new Dependency(Integer.class, List.of(), 0).key())
                .isEqualTo("java.lang.Integer:");
        assertThat(new Dependency(Integer.class, List.of("bar"), 0).key())
                .isEqualTo("java.lang.Integer:bar");
    }

    // ── Static-init and cross-phase tests ──

    @Test
    void staticInitGraphValidatesIndependently() {
        // static-init services with satisfied deps within static graph
        ServiceDependencyValidator.validate(
                List.of(
                        new StaticServiceMetadataBuildItem(String.class, List.of(), List.of(), "StaticA#a"),
                        new StaticServiceMetadataBuildItem(Integer.class, List.of(),
                                List.of(new Dependency(String.class, List.of(), 0)),
                                "StaticB#b")),
                List.of());
    }

    @Test
    void runtimeCanDependOnStaticInitOutput() {
        // static provides String, runtime depends on it
        ServiceDependencyValidator.validate(
                List.of(
                        new StaticServiceMetadataBuildItem(String.class, List.of(), List.of(), "StaticA#a")),
                List.of(
                        rt(Integer.class, List.of(),
                                List.of(new Dependency(String.class, List.of(), 0)),
                                "RuntimeB#b")));
    }

    @Test
    void crossPhaseDuplicateIsError() {
        // same type+name in both phases
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(
                List.of(
                        new StaticServiceMetadataBuildItem(String.class, List.of(), List.of(), "StaticA#a")),
                List.of(
                        rt(String.class, List.of(), List.of(), "RuntimeB#b"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cross-phase duplicate")
                .hasMessageContaining("java.lang.String:");
    }

    @Test
    void staticInitCycleDetected() {
        // cycle within the static-init graph
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(
                List.of(
                        new StaticServiceMetadataBuildItem(String.class, List.of(),
                                List.of(new Dependency(Integer.class, List.of(), 0)),
                                "StaticA#a"),
                        new StaticServiceMetadataBuildItem(Integer.class, List.of(),
                                List.of(new Dependency(String.class, List.of(), 0)),
                                "StaticB#b")),
                List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cycle")
                .hasMessageContaining("static-init");
    }

    @Test
    void staticCannotDependOnRuntimeOutput() {
        // static service depends on something only provided by runtime — should fail
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(
                List.of(
                        new StaticServiceMetadataBuildItem(Integer.class, List.of(),
                                List.of(new Dependency(String.class, List.of(), 0)),
                                "StaticA#a")),
                List.of(
                        rt(String.class, List.of(), List.of(), "RuntimeB#b"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsatisfied dependency")
                .hasMessageContaining("static-init");
    }

    @Test
    void staticDuplicateWithinPhaseDetected() {
        // two static-init services with same key
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(
                List.of(
                        new StaticServiceMetadataBuildItem(String.class, List.of(), List.of(), "StaticA#a"),
                        new StaticServiceMetadataBuildItem(String.class, List.of(), List.of(), "StaticB#b")),
                List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate static-init")
                .hasMessageContaining("java.lang.String:");
    }

    @Test
    void bothPhasesCanHaveDifferentTypesWithoutConflict() {
        // static provides String, runtime provides Integer — no conflict
        ServiceDependencyValidator.validate(
                List.of(
                        new StaticServiceMetadataBuildItem(String.class, List.of(), List.of(), "StaticA#a")),
                List.of(
                        rt(Integer.class, List.of(), List.of(), "RuntimeB#b")));
    }

    // ── Phase ordering validation tests ──

    @Test
    void samePhaseDepPasses() {
        ServiceDependencyValidator.validate(List.of(), List.of(
                new ServiceMetadataBuildItem(String.class, List.of(), List.of(), "StepA#a", Phase.DATA),
                new ServiceMetadataBuildItem(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of(), 0)),
                        "StepB#b", Phase.DATA)));
    }

    @Test
    void earlierPhaseDepPasses() {
        ServiceDependencyValidator.validate(List.of(), List.of(
                new ServiceMetadataBuildItem(String.class, List.of(), List.of(), "StepA#a", Phase.INFRASTRUCTURE),
                new ServiceMetadataBuildItem(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of(), 0)),
                        "StepB#b", Phase.DATA)));
    }

    @Test
    void laterPhaseDepIsError() {
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(List.of(), List.of(
                new ServiceMetadataBuildItem(String.class, List.of(), List.of(), "StepA#a", Phase.APPLICATION),
                new ServiceMetadataBuildItem(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of(), 0)),
                        "StepB#b", Phase.INFRASTRUCTURE))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Phase ordering violation")
                .hasMessageContaining("INFRASTRUCTURE")
                .hasMessageContaining("APPLICATION");
    }

    @Test
    void optionalDepToLaterPhaseIsStillError() {
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(List.of(), List.of(
                new ServiceMetadataBuildItem(String.class, List.of(), List.of(), "StepA#a", Phase.SERVING),
                new ServiceMetadataBuildItem(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of(), Dependency.FL_INJECTED | Dependency.FL_OPTIONAL)),
                        "StepB#b", Phase.CONFIG))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Phase ordering violation");
    }

    @Test
    void afterDepToLaterPhaseIsError() {
        assertThatThrownBy(() -> ServiceDependencyValidator.validate(List.of(), List.of(
                new ServiceMetadataBuildItem(String.class, List.of(), List.of(), "StepA#a", Phase.APPLICATION),
                new ServiceMetadataBuildItem(Integer.class, List.of(),
                        List.of(new Dependency(String.class, List.of(), Dependency.FL_OPTIONAL)),
                        "StepB#b", Phase.INFRASTRUCTURE))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Phase ordering violation");
    }
}

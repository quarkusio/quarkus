package io.quarkus.platform.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.platform.tools.ExtensionStructure.ClassFacts;
import io.quarkus.platform.tools.ExtensionStructureChecker.Violation;

class ExtensionStructureCheckerTest {

    @Test
    void acceptsExtensionFollowingTheStructure() {
        ExtensionStructure structure = new ExtensionStructure("io.quarkus", "quarkus-foo", "quarkus-foo-deployment",
                List.of(plain("io.quarkus.foo.Foo"),
                        plain("io.quarkus.foo.runtime.internal.FooRecorder"),
                        nativeImageHook("io.quarkus.foo.runtime.graal.FooSubstitutions")),
                List.of(buildSteps("io.quarkus.foo.deployment.FooProcessor"),
                        plain("io.quarkus.foo.deployment.items.FooBuildItem")));

        assertThat(ExtensionStructureChecker.check(structure)).isEmpty();
    }

    @Test
    void reportsRuntimeClassesOutsideTheRuntimePackages() {
        ExtensionStructure structure = new ExtensionStructure("io.quarkus", "quarkus-foo", "quarkus-foo-deployment",
                List.of(plain("io.quarkus.foo.Foo"),
                        plain("io.quarkus.foo.dev.FooDevService"),
                        plain("io.quarkus.foo.api.FooApi"),
                        plain("io.quarkus.foo.deployment.FooConfig"),
                        plain("io.quarkus.bar.Bar")),
                List.of());

        assertThat(ExtensionStructureChecker.check(structure))
                .singleElement()
                .satisfies(v -> assertThat(v.message()).contains("quarkus-foo", "io.quarkus.foo"))
                .extracting(Violation::offenders).asInstanceOf(LIST)
                .containsExactly("io.quarkus.foo.dev.FooDevService", "io.quarkus.foo.api.FooApi",
                        "io.quarkus.foo.deployment.FooConfig", "io.quarkus.bar.Bar");
    }

    @Test
    void reportsDeploymentClassesOutsideTheDeploymentPackage() {
        ExtensionStructure structure = new ExtensionStructure("io.quarkus", "quarkus-foo", "quarkus-foo-deployment",
                List.of(),
                List.of(plain("io.quarkus.foo.deployment.FooConfig"),
                        plain("io.quarkus.foo.FooHelper"),
                        plain("io.quarkus.foo.deployment.spi.FooBuildItem")));

        assertThat(offenders(structure))
                .containsExactly(List.of("io.quarkus.foo.FooHelper", "io.quarkus.foo.deployment.spi.FooBuildItem"));
    }

    @Test
    void acceptsBothRootPackageSpellingsForHyphenatedNames() {
        ExtensionStructure structure = new ExtensionStructure("io.quarkus", "quarkus-foo-bar", "quarkus-foo-bar-deployment",
                List.of(plain("io.quarkus.foo_bar.FooBar"), plain("io.quarkus.foo.bar.runtime.FooBarRecorder")),
                List.of(buildSteps("io.quarkus.foo.bar.deployment.FooBarProcessor")));

        assertThat(ExtensionStructureChecker.check(structure)).isEmpty();
    }

    @Test
    void derivesTheQuarkiverseRootPackageFromTheGroupId() {
        ExtensionStructure structure = new ExtensionStructure("io.quarkiverse.foo", "quarkus-foo", "quarkus-foo-deployment",
                List.of(plain("io.quarkiverse.foo.runtime.FooRecorder"), plain("io.quarkus.foo.Foo")),
                List.of());

        assertThat(offenders(structure)).containsExactly(List.of("io.quarkus.foo.Foo"));
    }

    @Test
    void skipsPackageRulesWhenTheRootPackageCannotBeDerived() {
        ExtensionStructure structure = new ExtensionStructure("org.apache.camel.quarkus", "quarkus-foo",
                "quarkus-foo-deployment",
                List.of(plain("org.apache.camel.quarkus.component.foo.FooRecorder")),
                List.of(plain("org.apache.camel.quarkus.component.foo.deployment.FooProcessor")));

        assertThat(ExtensionStructureChecker.check(structure)).isEmpty();
    }

    @Test
    void reportsBuildStepsInTheRuntimeModule() {
        ExtensionStructure structure = new ExtensionStructure("io.quarkus", "quarkus-foo", "quarkus-foo-deployment",
                List.of(buildSteps("io.quarkus.foo.runtime.FooSteps")),
                List.of());

        assertThat(offenders(structure)).containsExactly(List.of("io.quarkus.foo.runtime.FooSteps"));
    }

    @Test
    void reportsBuildStepsOutsideProcessorClasses() {
        ExtensionStructure structure = new ExtensionStructure("io.quarkus", "quarkus-foo", "quarkus-foo-deployment",
                List.of(),
                List.of(buildSteps("io.quarkus.foo.deployment.FooProcessor"),
                        buildSteps("io.quarkus.foo.deployment.FooSteps")));

        assertThat(offenders(structure)).containsExactly(List.of("io.quarkus.foo.deployment.FooSteps"));
    }

    @Test
    void reportsNativeImageHooksOutsideTheGraalPackage() {
        ExtensionStructure structure = new ExtensionStructure("io.quarkus", "quarkus-foo", "quarkus-foo-deployment",
                List.of(nativeImageHook("io.quarkus.foo.runtime.FooFeature")),
                List.of());

        assertThat(ExtensionStructureChecker.check(structure))
                .singleElement()
                .satisfies(v -> assertThat(v.message()).contains("io.quarkus.foo.runtime.graal"))
                .extracting(Violation::offenders).asInstanceOf(LIST)
                .containsExactly("io.quarkus.foo.runtime.FooFeature");
    }

    @Test
    void reportsDeploymentArtifactIdNotMatchingTheRuntimeOne() {
        ExtensionStructure structure = new ExtensionStructure("io.quarkus", "quarkus-foo", "quarkus-foo-processor",
                List.of(),
                List.of());

        assertThat(ExtensionStructureChecker.check(structure))
                .singleElement()
                .satisfies(v -> assertThat(v.message()).contains("quarkus-foo-deployment"))
                .extracting(Violation::offenders).asInstanceOf(LIST)
                .containsExactly("quarkus-foo-processor");
    }

    @Test
    void reportsRuntimeArtifactIdWithoutTheQuarkusPrefix() {
        ExtensionStructure structure = new ExtensionStructure("io.quarkus", "foo", "foo-deployment",
                List.of(),
                List.of());

        assertThat(offenders(structure)).containsExactly(List.of("foo"));
    }

    @Test
    void describesOnlyTheFirstOffenders() {
        Violation violation = new Violation("Classes are misplaced",
                List.of("A", "B", "C", "D", "E", "F", "G"));

        assertThat(violation.describe()).isEqualTo("Classes are misplaced: A, B, C, D, E and 2 more");
    }

    private static List<List<String>> offenders(ExtensionStructure structure) {
        return ExtensionStructureChecker.check(structure).stream().map(Violation::offenders).toList();
    }

    private static ClassFacts plain(String name) {
        return new ClassFacts(name, false, false);
    }

    private static ClassFacts buildSteps(String name) {
        return new ClassFacts(name, true, false);
    }

    private static ClassFacts nativeImageHook(String name) {
        return new ClassFacts(name, false, true);
    }
}

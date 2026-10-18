package io.quarkus.avro.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.avro.runtime.QuarkusAvroClassSecurityPredicate;

class QuarkusAvroClassSecurityPredicateTest {

    static class PlainClass {
    }

    static class Outer {
        static class Inner {
        }
    }

    @Test
    void trustsExplicitlyListedClasses() {
        QuarkusAvroClassSecurityPredicate predicate = new QuarkusAvroClassSecurityPredicate(
                Set.of(PlainClass.class.getName()), List.of());
        assertThat(predicate.isTrusted(PlainClass.class)).isTrue();
    }

    @Test
    void trustsClassesInConfiguredPackage() {
        // PlainClass lives in io.quarkus.avro.deployment
        QuarkusAvroClassSecurityPredicate predicate = new QuarkusAvroClassSecurityPredicate(Set.of(),
                List.of("io.quarkus.avro.deployment"));
        assertThat(predicate.isTrusted(PlainClass.class)).isTrue();
    }

    @Test
    void trustsClassesInSubPackagesOfConfiguredPackage() {
        // io.quarkus.avro.deployment is a sub-package of io.quarkus.avro
        QuarkusAvroClassSecurityPredicate predicate = new QuarkusAvroClassSecurityPredicate(Set.of(),
                List.of("io.quarkus.avro"));
        assertThat(predicate.isTrusted(PlainClass.class)).isTrue();
    }

    @Test
    void packageMatchingRespectsPackageBoundaries() {
        // "io.quarkus.avro.dep" is a raw string prefix of "io.quarkus.avro.deployment", but NOT a package-boundary
        // prefix, so classes in the deployment package must NOT be trusted. This mirrors the "org.acme" vs "org.acme2"
        // case: configuring "org.acme" must not trust "org.acme2".
        QuarkusAvroClassSecurityPredicate predicate = new QuarkusAvroClassSecurityPredicate(Set.of(),
                List.of("io.quarkus.avro.dep"));
        assertThat(predicate.isTrusted(PlainClass.class)).isFalse();
    }

    @Test
    void doesNotTrustUnknownClasses() {
        QuarkusAvroClassSecurityPredicate predicate = new QuarkusAvroClassSecurityPredicate(Set.of(), List.of());
        assertThat(predicate.isTrusted(PlainClass.class)).isFalse();
        assertThat(predicate.isTrusted(File.class)).isFalse();
    }

    @Test
    void trustingAClassDoesNotImplicitlyTrustItsNestedClasses() {
        // Aligned with Avro: trusted-class matching is an exact Class.getName() match, and a nested class has a
        // distinct binary name (e.g. "...$Outer$Inner"), so trusting the outer class does not trust the inner one.
        QuarkusAvroClassSecurityPredicate predicate = new QuarkusAvroClassSecurityPredicate(
                Set.of(Outer.class.getName()), List.of());
        assertThat(predicate.isTrusted(Outer.class)).isTrue();
        assertThat(predicate.isTrusted(Outer.Inner.class)).isFalse();
    }

    @Test
    void trustsNestedClassesListedByTheirBinaryName() {
        QuarkusAvroClassSecurityPredicate predicate = new QuarkusAvroClassSecurityPredicate(
                Set.of(Outer.Inner.class.getName()), List.of());
        assertThat(predicate.isTrusted(Outer.Inner.class)).isTrue();
    }

    @Test
    void trustsNestedClassesViaConfiguredPackage() {
        // A nested class keeps the package prefix in its binary name, so package trust covers it (as in Avro).
        QuarkusAvroClassSecurityPredicate predicate = new QuarkusAvroClassSecurityPredicate(Set.of(),
                List.of("io.quarkus.avro.deployment"));
        assertThat(predicate.isTrusted(Outer.Inner.class)).isTrue();
    }
}

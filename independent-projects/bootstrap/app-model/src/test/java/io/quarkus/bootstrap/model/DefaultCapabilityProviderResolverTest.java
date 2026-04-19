package io.quarkus.bootstrap.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;

class DefaultCapabilityProviderResolverTest {

    @Test
    void noRequirementsReturnsNull() {
        var resolver = new DefaultCapabilityProviderResolver(Map.of());
        assertThat(resolver.getNextDefaultProvider()).isNull();
    }

    @Test
    void satisfiedRequirementReturnsNull() {
        var resolver = new DefaultCapabilityProviderResolver(Map.of(
                "cap.a", ArtifactCoords.fromString("org.acme:provider-a::jar:1.0")));
        resolver.registerProvided("cap.a", "org.acme:ext-a");
        resolver.registerRequired("cap.a", "org.acme:ext-b", () -> new int[] { 0 });
        assertThat(resolver.getNextDefaultProvider()).isNull();
    }

    @Test
    void unsatisfiedRequirementWithDefaultProvider() {
        var providerCoords = ArtifactCoords.fromString("org.acme:provider-a::jar:1.0");
        var resolver = new DefaultCapabilityProviderResolver(Map.of("cap.a", providerCoords));
        resolver.registerRequired("cap.a", "org.acme:ext-b", () -> new int[] { 0 });
        assertThat(resolver.getNextDefaultProvider()).isEqualTo(providerCoords);
    }

    @Test
    void unsatisfiedRequirementWithNoDefaultProviderReturnsNull() {
        var resolver = new DefaultCapabilityProviderResolver(Map.of());
        resolver.registerRequired("cap.a", "org.acme:ext-b", () -> new int[] { 0 });
        assertThat(resolver.getNextDefaultProvider()).isNull();
    }

    @Test
    void priorityOrderingShallowerDepthFirst() {
        var providerA = ArtifactCoords.fromString("org.acme:provider-a::jar:1.0");
        var providerB = ArtifactCoords.fromString("org.acme:provider-b::jar:1.0");
        var resolver = new DefaultCapabilityProviderResolver(Map.of(
                "cap.a", providerA,
                "cap.b", providerB));
        resolver.registerRequired("cap.a", "org.acme:ext-deep", () -> new int[] { 0, 0, 0 });
        resolver.registerRequired("cap.b", "org.acme:ext-shallow", () -> new int[] { 0 });
        assertThat(resolver.getNextDefaultProvider()).isEqualTo(providerB);
    }

    @Test
    void priorityOrderingSameDepthEarlierPathFirst() {
        var providerA = ArtifactCoords.fromString("org.acme:provider-a::jar:1.0");
        var providerB = ArtifactCoords.fromString("org.acme:provider-b::jar:1.0");
        var resolver = new DefaultCapabilityProviderResolver(Map.of(
                "cap.a", providerA,
                "cap.b", providerB));
        // ext-second is at path [1, 2] (second subtree, third child)
        resolver.registerRequired("cap.a", "org.acme:ext-second", () -> new int[] { 1, 2 });
        // ext-first is at path [0, 1] (first subtree, second child)
        resolver.registerRequired("cap.b", "org.acme:ext-first", () -> new int[] { 0, 1 });
        assertThat(resolver.getNextDefaultProvider()).isEqualTo(providerB);
    }

    @Test
    void providerSatisfyingCapabilityIsNotReturnedAgain() {
        var providerCoords = ArtifactCoords.fromString("org.acme:provider-a::jar:1.0");
        var resolver = new DefaultCapabilityProviderResolver(Map.of("cap.a", providerCoords));
        resolver.registerRequired("cap.a", "org.acme:ext-b", () -> new int[] { 0 });

        assertThat(resolver.getNextDefaultProvider()).isNotNull();

        // simulate the provider being added and its capabilities registered
        resolver.registerProvided("cap.a", "org.acme:provider-a");

        assertThat(resolver.getNextDefaultProvider()).isNull();
    }

    @Test
    void multipleExtensionsRequiringSameCapability() {
        var providerCoords = ArtifactCoords.fromString("org.acme:provider-a::jar:1.0");
        var resolver = new DefaultCapabilityProviderResolver(Map.of("cap.a", providerCoords));
        resolver.registerRequired("cap.a", "org.acme:ext-b", () -> new int[] { 0 });
        resolver.registerRequired("cap.a", "org.acme:ext-c", () -> new int[] { 0, 0 });

        assertThat(resolver.getNextDefaultProvider()).isEqualTo(providerCoords);

        // after injection + registration, both are satisfied
        resolver.registerProvided("cap.a", "org.acme:provider-a");
        assertThat(resolver.getNextDefaultProvider()).isNull();
    }

    @Test
    void parseUnconditionalCapabilitiesFiltersConditional() {
        var result = DefaultCapabilityProviderResolver.parseUnconditionalCapabilities(
                "cap.a, cap.b?io.quarkus.SomeSupplier, cap.c");
        assertThat(result).containsExactly("cap.a", "cap.c");
    }

    @Test
    void parseUnconditionalCapabilitiesNullInput() {
        assertThat(DefaultCapabilityProviderResolver.parseUnconditionalCapabilities(null)).isEmpty();
    }

    @Test
    void parseUnconditionalCapabilitiesBlankInput() {
        assertThat(DefaultCapabilityProviderResolver.parseUnconditionalCapabilities("  ")).isEmpty();
    }

    @Test
    void parseUnconditionalCapabilitiesAllConditional() {
        var result = DefaultCapabilityProviderResolver.parseUnconditionalCapabilities(
                "cap.a?Supplier1, cap.b?!Supplier2");
        assertThat(result).isEmpty();
    }

    @Test
    void parseUnconditionalCapabilitiesNoConditional() {
        var result = DefaultCapabilityProviderResolver.parseUnconditionalCapabilities("cap.a, cap.b, cap.c");
        assertThat(result).containsExactly("cap.a", "cap.b", "cap.c");
    }

    @Test
    void hasDefaultProvidersFalseWhenEmpty() {
        assertThat(new DefaultCapabilityProviderResolver(Map.of()).hasDefaultProviders()).isFalse();
    }

    @Test
    void hasDefaultProvidersTrueWhenNonEmpty() {
        assertThat(new DefaultCapabilityProviderResolver(
                Map.of("cap.a", ArtifactCoords.fromString("org.acme:a::jar:1.0"))).hasDefaultProviders()).isTrue();
    }

    @Test
    void injectionCountLimitPreventsInfiniteLoop() {
        var providerCoords = ArtifactCoords.fromString("org.acme:provider-a::jar:1.0");
        var resolver = new DefaultCapabilityProviderResolver(Map.of("cap.a", providerCoords));
        resolver.registerRequired("cap.a", "org.acme:ext-b", () -> new int[] { 0 });

        // first call returns the provider
        assertThat(resolver.getNextDefaultProvider()).isNotNull();

        // simulate the provider NOT registering its capability (broken descriptor)
        // second call should return null due to the injection count limit
        assertThat(resolver.getNextDefaultProvider()).isNull();
    }

    @Test
    void requirementDiscoveredInLaterCycleWithCorrectDepth() {
        var providerA = ArtifactCoords.fromString("org.acme:provider-a::jar:1.0");
        var providerB = ArtifactCoords.fromString("org.acme:provider-b::jar:1.0");
        var resolver = new DefaultCapabilityProviderResolver(Map.of(
                "cap.a", providerA,
                "cap.b", providerB));

        // first cycle: deep requirement
        resolver.registerRequired("cap.a", "org.acme:ext-deep", () -> new int[] { 0, 0, 0, 0 });

        assertThat(resolver.getNextDefaultProvider()).isEqualTo(providerA);
        resolver.registerProvided("cap.a", "org.acme:provider-a");

        // second cycle: new shallow requirement discovered
        resolver.registerRequired("cap.b", "org.acme:ext-shallow", () -> new int[] { 0 });

        assertThat(resolver.getNextDefaultProvider()).isEqualTo(providerB);
    }
}

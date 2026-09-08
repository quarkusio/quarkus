package io.quarkus.cyclonedx.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.component.evidence.Identity;
import org.cyclonedx.parsers.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.resolver.maven.EffectiveModelResolver;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;
import io.quarkus.sbom.ComponentDependencies;
import io.quarkus.sbom.ComponentDescriptor;
import io.quarkus.sbom.CoreSbomContributionConfig;
import io.quarkus.sbom.LicenseInfo;
import io.quarkus.sbom.Purl;
import io.quarkus.sbom.SbomContribution;

class CycloneDxSbomGeneratorTest {

    @Test
    void topLevelComponentsLinkedToMainComponent() {
        // Core contribution via CoreSbomContributionConfig (sets mainComponentBomRef)
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0",
                List.of(ArtifactCoords.jar("io.quarkus", "quarkus-rest", "3.0.0")));
        ResolvedDependency restDep = resolvedDep("io.quarkus", "quarkus-rest", "3.0.0", List.of());

        SbomContribution coreContribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(restDep)
                .toSbomContribution();

        String mainBomRef = coreContribution.mainComponentBomRef();

        // Extension contribution: 2 npm packages, 1 top-level + 1 transitive
        ComponentDescriptor react = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .setTopLevel(true)
                .build();
        ComponentDescriptor jsTokens = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "js-tokens", "4.0.0"))
                .build();
        SbomContribution extensionContribution = SbomContribution.of(
                List.of(react, jsTokens),
                List.of(ComponentDependencies.of(
                        react.getBomRef(),
                        List.of(jsTokens.getBomRef()))));

        // Generate SBOM text (JSON)
        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(coreContribution, extensionContribution))
                .generateText();

        assertThat(result).hasSize(1);

        // Parse the output back to verify dependency structure
        Bom bom = parseBom(result.get(0));

        // Find main component dependency entry
        Dependency mainDep = bom.getDependencies().stream()
                .filter(d -> d.getRef().equals(mainBomRef))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Main component not in dependency graph"));

        List<String> mainDependsOn = mainDep.getDependencies().stream()
                .map(Dependency::getRef)
                .toList();

        // Should include react (top-level extension component)
        assertThat(mainDependsOn).contains(react.getBomRef());
        // Should NOT include js-tokens (transitive, not top-level)
        assertThat(mainDependsOn).doesNotContain(jsTokens.getBomRef());
    }

    @Test
    void noMainComponentSkipsTopLevelLinking() {
        // Extension-only contribution with no main component
        ComponentDescriptor react = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .setTopLevel(true)
                .build();
        SbomContribution contribution = SbomContribution.ofComponents(List.of(react));

        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(contribution))
                .generateText();

        assertThat(result).hasSize(1);
        // Should not crash — no main component to link to
        assertThat(result.get(0)).contains("react");
    }

    @Test
    void descriptorLicenseResolvedInSbom() {
        ComponentDescriptor react = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .addLicense(new LicenseInfo("MIT"))
                .build();
        SbomContribution contribution = SbomContribution.ofComponents(List.of(react));

        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(contribution))
                .generateText();

        Bom bom = parseBom(result.get(0));
        org.cyclonedx.model.Component component = bom.getComponents().stream()
                .filter(c -> c.getName().equals("react"))
                .findFirst()
                .orElseThrow();
        assertThat(component.getLicenses().getLicenses())
                .isNotEmpty()
                .anyMatch(l -> "MIT".equals(l.getId()) || "MIT".equals(l.getName()));
    }

    @Test
    void descriptorLicenseWithUrlResolvedInSbom() {
        ComponentDescriptor pkg = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "my-pkg", "1.0.0"))
                .addLicense(new LicenseInfo("CustomLicense", "https://example.com/license"))
                .build();
        SbomContribution contribution = SbomContribution.ofComponents(List.of(pkg));

        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(contribution))
                .generateText();

        Bom bom = parseBom(result.get(0));
        org.cyclonedx.model.Component component = bom.getComponents().stream()
                .filter(c -> c.getName().equals("my-pkg"))
                .findFirst()
                .orElseThrow();
        assertThat(component.getLicenses().getLicenses())
                .isNotEmpty()
                .anyMatch(l -> "CustomLicense".equals(l.getName())
                        && "https://example.com/license".equals(l.getUrl()));
    }

    @Test
    void descriptorMultipleLicensesResolvedInSbom() {
        ComponentDescriptor pkg = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "dual-licensed", "2.0.0"))
                .addLicense(new LicenseInfo("MIT"))
                .addLicense(new LicenseInfo("Apache-2.0"))
                .build();
        SbomContribution contribution = SbomContribution.ofComponents(List.of(pkg));

        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(contribution))
                .generateText();

        Bom bom = parseBom(result.get(0));
        org.cyclonedx.model.Component component = bom.getComponents().stream()
                .filter(c -> c.getName().equals("dual-licensed"))
                .findFirst()
                .orElseThrow();
        List<String> licenseIds = component.getLicenses().getLicenses().stream()
                .map(l -> l.getId() != null ? l.getId() : l.getName())
                .toList();
        assertThat(licenseIds).contains("MIT", "Apache-2.0");
    }

    @Test
    void leafComponentsHaveDependencyEntries() {
        ComponentDescriptor react = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .build();
        ComponentDescriptor jsTokens = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "js-tokens", "4.0.0"))
                .build();
        SbomContribution contribution = SbomContribution.of(
                List.of(react, jsTokens),
                List.of(ComponentDependencies.of(
                        react.getBomRef(),
                        List.of(jsTokens.getBomRef()))));

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(contribution))
                .generateText().get(0));

        // js-tokens is a leaf with no dependencies of its own,
        // but it should still have an entry in the dependency graph
        assertThat(bom.getDependencies())
                .extracting(Dependency::getRef)
                .contains(react.getBomRef(), jsTokens.getBomRef());

        Dependency jsTokensDep = bom.getDependencies().stream()
                .filter(d -> d.getRef().equals(jsTokens.getBomRef()))
                .findFirst()
                .orElseThrow();
        assertThat(jsTokensDep.getDependencies()).isNullOrEmpty();
    }

    @Test
    void serialNumberIsDeterministicWithOutputTimestamp() {
        Instant timestamp = Instant.parse("2025-01-15T10:00:00Z");
        ComponentDescriptor react = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .build();
        SbomContribution contribution = SbomContribution.ofComponents(List.of(react));

        Bom bom1 = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setOutputTimestamp(timestamp)
                .setContributions(List.of(contribution))
                .generateText().get(0));
        Bom bom2 = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setOutputTimestamp(timestamp)
                .setContributions(List.of(contribution))
                .generateText().get(0));

        // pinned to a literal on purpose; we need to make sure that the serial number is deterministic
        assertThat(bom1.getSerialNumber())
                .isEqualTo("urn:uuid:9d197c65-1a43-3cf5-bb49-5a88553d1dfc")
                .isEqualTo(bom2.getSerialNumber());
    }

    @Test
    void serialNumberChangesWhenComponentsChange() {
        Instant timestamp = Instant.parse("2025-01-15T10:00:00Z");
        ComponentDescriptor react = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .build();
        ComponentDescriptor vue = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "vue", "3.0.0"))
                .build();

        Bom bom1 = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setOutputTimestamp(timestamp)
                .setContributions(List.of(SbomContribution.ofComponents(List.of(react))))
                .generateText().get(0));

        Bom bom2 = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setOutputTimestamp(timestamp)
                .setContributions(List.of(SbomContribution.ofComponents(List.of(react, vue))))
                .generateText().get(0));

        assertThat(bom1.getSerialNumber()).isNotEqualTo(bom2.getSerialNumber());
    }

    @Test
    void serialNumberIsSetWithoutOutputTimestamp() {
        ComponentDescriptor react = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .build();

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(SbomContribution.ofComponents(List.of(react))))
                .generateText().get(0));

        assertThat(bom.getSerialNumber())
                .isNotNull()
                .startsWith("urn:uuid:");
    }

    @Test
    void nestedComponentsRenderedAsBundledComponents() {
        ComponentDescriptor nested = ComponentDescriptor.builder()
                .setPurl(Purl.maven("org.bundled", "bundled-dep", "2.0.0", "jar", null))
                .setBomRef("pkg:maven/org.bundled/bundled-dep@2.0.0?type=jar#bundled")
                .build();

        ComponentDescriptor parent = ComponentDescriptor.builder()
                .setPurl(Purl.maven("com.example", "shaded-lib", "1.0.0", "jar", null))
                .addComponent(nested)
                .build();

        SbomContribution contribution = SbomContribution.ofComponents(List.of(parent));

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(contribution))
                .generateText().get(0));

        org.cyclonedx.model.Component parentComp = bom.getComponents().stream()
                .filter(c -> c.getName().equals("shaded-lib"))
                .findFirst()
                .orElseThrow();

        assertThat(parentComp.getComponents())
                .as("Parent should contain nested bundled component")
                .hasSize(1);

        org.cyclonedx.model.Component nestedComp = parentComp.getComponents().get(0);
        assertThat(nestedComp.getName()).isEqualTo("bundled-dep");
        assertThat(nestedComp.getGroup()).isEqualTo("org.bundled");
        assertThat(nestedComp.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void unresolvableBundledComponentPomDoesNotFailGeneration() {
        // https://github.com/quarkusio/quarkus/issues/55373
        // org.jacoco:org.jacoco.agent:runtime bundles org.jacoco.agent.rt, whose POM
        // is never published to Maven Central. Resolving POM metadata for such bundled
        // components must not fail the SBOM generation.
        ComponentDescriptor nested = ComponentDescriptor.builder()
                .setPurl(Purl.maven("org.jacoco", "org.jacoco.agent.rt", "0.8.15", "jar", null))
                .setBomRef("pkg:maven/org.jacoco/org.jacoco.agent.rt@0.8.15?type=jar#bundled")
                .build();
        ComponentDescriptor parent = ComponentDescriptor.builder()
                .setPurl(Purl.maven("org.jacoco", "org.jacoco.agent", "0.8.15", "jar", "runtime"))
                .addComponent(nested)
                .build();

        EffectiveModelResolver resolver = (coords, repos) -> {
            if (coords.getArtifactId().equals("org.jacoco.agent.rt")) {
                throw new RuntimeException("Failed to resolve " + coords.toCompactCoords());
            }
            return null;
        };

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setEffectiveModelResolver(resolver)
                .setContributions(List.of(SbomContribution.ofComponents(List.of(parent))))
                .generateText().get(0));

        org.cyclonedx.model.Component parentComp = bom.getComponents().stream()
                .filter(c -> c.getName().equals("org.jacoco.agent"))
                .findFirst()
                .orElseThrow();
        assertThat(parentComp.getComponents()).hasSize(1);
        assertThat(parentComp.getComponents().get(0).getName()).isEqualTo("org.jacoco.agent.rt");
    }

    @Test
    void librariesOnlyExcludesFileComponents(@TempDir Path tempDir) throws Exception {
        // Create a temporary file so the descriptor has a real path
        Path runnerJar = tempDir.resolve("quarkus-run.jar");
        Files.createFile(runnerJar);

        // Main component (APPLICATION type, should be kept)
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0",
                List.of(ArtifactCoords.jar("io.quarkus", "quarkus-rest", "3.0.0")));
        ResolvedDependency restDep = resolvedDep("io.quarkus", "quarkus-rest", "3.0.0", List.of());

        SbomContribution coreContribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(restDep)
                .toSbomContribution();

        // FILE component: generic PURL with a distribution path
        ComponentDescriptor fileComponent = ComponentDescriptor.builder()
                .setPurl(Purl.generic("quarkus-run.jar", "1.0.0"))
                .setDistributionPath("quarkus-run.jar")
                .build();

        // FILE component: generic PURL with a file system path
        ComponentDescriptor fileWithPath = ComponentDescriptor.builder()
                .setPurl(Purl.generic("app-data.dat", "1.0.0"))
                .setPath(runnerJar)
                .build();

        // LIBRARY component: npm package (should be kept)
        ComponentDescriptor npmLib = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .build();

        SbomContribution extContribution = SbomContribution.of(
                List.of(fileComponent, fileWithPath, npmLib),
                List.of(ComponentDependencies.of(
                        fileComponent.getBomRef(),
                        List.of(npmLib.getBomRef()))));

        // Generate with librariesOnly=true
        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setLibrariesOnly(true)
                .setContributions(List.of(coreContribution, extContribution))
                .generateText().get(0));

        List<String> componentNames = bom.getComponents().stream()
                .map(Component::getName)
                .toList();

        // Library and main components should be present
        assertThat(componentNames).contains("react", "quarkus-rest", "acme-app");
        // File components should be excluded
        assertThat(componentNames).doesNotContain("quarkus-run.jar", "app-data.dat");

        // Dependency graph should not reference excluded components
        List<String> allDepRefs = bom.getDependencies().stream()
                .map(Dependency::getRef)
                .toList();
        assertThat(allDepRefs).doesNotContain(fileComponent.getBomRef(), fileWithPath.getBomRef());

        // References to excluded components should be removed from other entries' dependsOn
        for (Dependency dep : bom.getDependencies()) {
            if (dep.getDependencies() != null) {
                List<String> depRefs = dep.getDependencies().stream()
                        .map(Dependency::getRef)
                        .toList();
                assertThat(depRefs)
                        .as("Dependency %s should not reference excluded file components", dep.getRef())
                        .doesNotContain(fileComponent.getBomRef(), fileWithPath.getBomRef());
            }
        }
    }

    @Test
    void librariesOnlyDisabledIncludesFileComponents() {
        // FILE component: generic PURL with a distribution path
        ComponentDescriptor fileComponent = ComponentDescriptor.builder()
                .setPurl(Purl.generic("quarkus-run.jar", "1.0.0"))
                .setDistributionPath("quarkus-run.jar")
                .build();

        ComponentDescriptor npmLib = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .build();

        SbomContribution contribution = SbomContribution.ofComponents(List.of(fileComponent, npmLib));

        // Generate with librariesOnly=false (default)
        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setLibrariesOnly(false)
                .setContributions(List.of(contribution))
                .generateText().get(0));

        List<String> componentNames = bom.getComponents().stream()
                .map(Component::getName)
                .toList();

        // Both should be present when librariesOnly is off
        assertThat(componentNames).contains("react", "quarkus-run.jar");
    }

    @Test
    void developmentScopeRenderedAsExcluded() {
        ComponentDescriptor mavenRuntime = ComponentDescriptor.builder()
                .setPurl(Purl.maven("io.quarkus", "quarkus-rest", "3.0.0", "jar", null))
                .setScope(ComponentDescriptor.SCOPE_RUNTIME)
                .build();
        ComponentDescriptor mavenDev = ComponentDescriptor.builder()
                .setPurl(Purl.maven("io.quarkus", "quarkus-rest-deployment", "3.0.0", "jar", null))
                .setScope(ComponentDescriptor.SCOPE_DEVELOPMENT)
                .build();
        ComponentDescriptor npmRuntime = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .setScope(ComponentDescriptor.SCOPE_RUNTIME)
                .build();
        ComponentDescriptor npmDev = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "eslint", "9.0.0"))
                .setScope(ComponentDescriptor.SCOPE_DEVELOPMENT)
                .build();
        ComponentDescriptor noScopeComp = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .build();

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(SbomContribution.ofComponents(
                        List.of(mavenRuntime, mavenDev, npmRuntime, npmDev, noScopeComp))))
                .generateText().get(0));

        Component mvnRt = findComponent(bom, "quarkus-rest");
        Component mvnDev = findComponent(bom, "quarkus-rest-deployment");
        Component npmRt = findComponent(bom, "react");
        Component npmDv = findComponent(bom, "eslint");
        Component noScope = findComponent(bom, "lodash");

        assertThat(mvnDev.getScope()).isEqualTo(Component.Scope.EXCLUDED);
        assertThat(npmDv.getScope()).isEqualTo(Component.Scope.EXCLUDED);
        assertThat(mvnRt.getScope()).isNull();
        assertThat(npmRt.getScope()).isNull();
        assertThat(noScope.getScope()).isNull();

        // quarkus:component:scope property should not be present by default
        assertThat(hasProperty(mvnDev, "quarkus:component:scope")).isFalse();
        assertThat(hasProperty(mvnRt, "quarkus:component:scope")).isFalse();
        assertThat(hasProperty(npmDv, "quarkus:component:scope")).isFalse();
        assertThat(hasProperty(npmRt, "quarkus:component:scope")).isFalse();
    }

    @Test
    void quarkusComponentScopePropertyIncludedWhenEnabled() {
        ComponentDescriptor mavenRuntime = ComponentDescriptor.builder()
                .setPurl(Purl.maven("io.quarkus", "quarkus-rest", "3.0.0", "jar", null))
                .setScope(ComponentDescriptor.SCOPE_RUNTIME)
                .build();
        ComponentDescriptor mavenDev = ComponentDescriptor.builder()
                .setPurl(Purl.maven("io.quarkus", "quarkus-rest-deployment", "3.0.0", "jar", null))
                .setScope(ComponentDescriptor.SCOPE_DEVELOPMENT)
                .build();
        ComponentDescriptor npmRuntime = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .setScope(ComponentDescriptor.SCOPE_RUNTIME)
                .build();
        ComponentDescriptor npmDev = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "eslint", "9.0.0"))
                .setScope(ComponentDescriptor.SCOPE_DEVELOPMENT)
                .build();

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setIncludeQuarkusComponentScope(true)
                .setContributions(List.of(SbomContribution.ofComponents(
                        List.of(mavenRuntime, mavenDev, npmRuntime, npmDev))))
                .generateText().get(0));

        assertThat(propertyValue(findComponent(bom, "quarkus-rest"), "quarkus:component:scope")).isEqualTo("runtime");
        assertThat(propertyValue(findComponent(bom, "quarkus-rest-deployment"), "quarkus:component:scope"))
                .isEqualTo("development");
        assertThat(propertyValue(findComponent(bom, "react"), "quarkus:component:scope")).isEqualTo("runtime");
        assertThat(propertyValue(findComponent(bom, "eslint"), "quarkus:component:scope")).isEqualTo("development");
    }

    @Test
    void runtimeOnlyExcludesDevelopmentComponents() {
        ComponentDescriptor mavenRuntime = ComponentDescriptor.builder()
                .setPurl(Purl.maven("io.quarkus", "quarkus-rest", "3.0.0", "jar", null))
                .setScope(ComponentDescriptor.SCOPE_RUNTIME)
                .build();
        ComponentDescriptor mavenDev = ComponentDescriptor.builder()
                .setPurl(Purl.maven("io.quarkus", "quarkus-rest-deployment", "3.0.0", "jar", null))
                .setScope(ComponentDescriptor.SCOPE_DEVELOPMENT)
                .build();
        ComponentDescriptor npmRuntime = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .setScope(ComponentDescriptor.SCOPE_RUNTIME)
                .build();
        ComponentDescriptor npmDev = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "eslint", "9.0.0"))
                .setScope(ComponentDescriptor.SCOPE_DEVELOPMENT)
                .build();
        ComponentDescriptor noScopeComp = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .build();

        SbomContribution contribution = SbomContribution.of(
                List.of(mavenRuntime, mavenDev, npmRuntime, npmDev, noScopeComp),
                List.of(
                        ComponentDependencies.of(
                                mavenRuntime.getBomRef(),
                                List.of(mavenDev.getBomRef())),
                        ComponentDependencies.of(
                                npmRuntime.getBomRef(),
                                List.of(npmDev.getBomRef(), noScopeComp.getBomRef()))));

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setRuntimeOnly(true)
                .setContributions(List.of(contribution))
                .generateText().get(0));

        List<String> componentNames = bom.getComponents().stream()
                .map(Component::getName)
                .toList();
        assertThat(componentNames).contains("quarkus-rest", "react", "lodash");
        assertThat(componentNames).doesNotContain("quarkus-rest-deployment", "eslint");

        List<String> allDepRefs = bom.getDependencies().stream()
                .map(Dependency::getRef)
                .toList();
        assertThat(allDepRefs).doesNotContain(mavenDev.getBomRef(), npmDev.getBomRef());

        Dependency mavenRtDep = bom.getDependencies().stream()
                .filter(d -> d.getRef().equals(mavenRuntime.getBomRef()))
                .findFirst()
                .orElseThrow();
        assertThat(mavenRtDep.getDependencies()).isNullOrEmpty();

        Dependency npmRtDep = bom.getDependencies().stream()
                .filter(d -> d.getRef().equals(npmRuntime.getBomRef()))
                .findFirst()
                .orElseThrow();
        List<String> npmRtDependsOn = npmRtDep.getDependencies().stream()
                .map(Dependency::getRef)
                .toList();
        assertThat(npmRtDependsOn).contains(noScopeComp.getBomRef());
        assertThat(npmRtDependsOn).doesNotContain(npmDev.getBomRef());
    }

    @Test
    void fileComponentsInheritProjectLicense() {
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution coreContribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .setMainPurl(Purl.generic("quarkus-run.jar", "1.0.0"))
                .addComponent(ComponentDescriptor.builder()
                        .setPurl(Purl.generic("appmodel.dat", "1.0.0"))
                        .setDistributionPath("lib/deployment/appmodel.dat")
                        .setDevelopmentScope())
                .toSbomContribution();

        // Model resolver returns a POM with Apache-2.0 license for the app artifact
        EffectiveModelResolver resolver = (coords, repos) -> {
            if ("acme-app".equals(coords.getArtifactId())) {
                return pomWithLicense("org.acme", "acme-app", "1.0.0", "Apache-2.0");
            }
            return null;
        };

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setEffectiveModelResolver(resolver)
                .setContributions(List.of(coreContribution))
                .generateText().get(0));

        Component fileComp = findComponent(bom, "appmodel.dat");
        assertThat(fileComp.getType()).isEqualTo(Component.Type.FILE);
        assertThat(fileComp.getLicenses()).isNotNull();
        assertThat(fileComp.getLicenses().getLicenses())
                .anyMatch(l -> "Apache-2.0".equals(l.getId()));
    }

    @Test
    void fileComponentsKeepOwnLicenseWhenPresent() {
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution coreContribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .setMainPurl(Purl.generic("quarkus-run.jar", "1.0.0"))
                .addComponent(ComponentDescriptor.builder()
                        .setPurl(Purl.generic("third-party.dat", "1.0.0"))
                        .setDistributionPath("data/third-party.dat")
                        .addLicense(new LicenseInfo("MIT")))
                .toSbomContribution();

        EffectiveModelResolver resolver = (coords, repos) -> {
            if ("acme-app".equals(coords.getArtifactId())) {
                return pomWithLicense("org.acme", "acme-app", "1.0.0", "Apache-2.0");
            }
            return null;
        };

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setEffectiveModelResolver(resolver)
                .setContributions(List.of(coreContribution))
                .generateText().get(0));

        Component fileComp = findComponent(bom, "third-party.dat");
        assertThat(fileComp.getLicenses().getLicenses())
                .hasSize(1)
                .anyMatch(l -> "MIT".equals(l.getId()) || "MIT".equals(l.getName()));
    }

    @Test
    void mainGenericComponentInheritsProjectLicense() {
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution coreContribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .setMainPurl(Purl.generic("quarkus-run.jar", "1.0.0"))
                .toSbomContribution();

        EffectiveModelResolver resolver = (coords, repos) -> {
            if ("acme-app".equals(coords.getArtifactId())) {
                return pomWithLicense("org.acme", "acme-app", "1.0.0", "Apache-2.0");
            }
            return null;
        };

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setEffectiveModelResolver(resolver)
                .setContributions(List.of(coreContribution))
                .generateText().get(0));

        Component main = bom.getMetadata().getComponent();
        assertThat(main.getType()).isEqualTo(Component.Type.APPLICATION);
        assertThat(main.getLicenses()).isNotNull();
        assertThat(main.getLicenses().getLicenses())
                .anyMatch(l -> "Apache-2.0".equals(l.getId()));
    }

    @Test
    void noProjectLicenseDoesNotFail() {
        ComponentDescriptor fileComponent = ComponentDescriptor.builder()
                .setPurl(Purl.generic("data.dat", "1.0.0"))
                .setDistributionPath("data.dat")
                .build();

        SbomContribution contribution = SbomContribution.ofComponents(List.of(fileComponent));

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(contribution))
                .generateText().get(0));

        Component fileComp = findComponent(bom, "data.dat");
        assertThat(fileComp.getType()).isEqualTo(Component.Type.FILE);
        assertThat(fileComp.getLicenses()).isNull();
    }

    private static org.apache.maven.model.Model pomWithLicense(String groupId, String artifactId,
            String version, String licenseName) {
        org.apache.maven.model.Model model = new org.apache.maven.model.Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
        org.apache.maven.model.License license = new org.apache.maven.model.License();
        license.setName(licenseName);
        model.addLicense(license);
        return model;
    }

    @Test
    void productComponentRendersFrameworkTypeCpeAndExcludedScope() {
        ComponentDescriptor product = ComponentDescriptor.builder()
                .setPurl(Purl.maven("com.redhat.quarkus.platform", "quarkus-camel-bom", "3.20.0", "pom", null))
                .setCpe("cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*")
                .setComponentType("framework")
                .setName("Camel Extensions for Quarkus")
                .setVersion("3.20")
                .setScope(ComponentDescriptor.SCOPE_EXCLUDED)
                .build();

        SbomContribution contribution = SbomContribution.ofComponents(List.of(product));

        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setSchemaVersion("1.6")
                .setContributions(List.of(contribution))
                .generateText();

        Bom bom = parseBom(result.get(0));
        Component c = bom.getComponents().stream()
                .filter(x -> product.getBomRef().equals(x.getBomRef()))
                .findFirst().orElseThrow();

        assertThat(c.getType()).isEqualTo(Component.Type.FRAMEWORK);
        assertThat(c.getName()).isEqualTo("Camel Extensions for Quarkus");
        assertThat(c.getVersion()).isEqualTo("3.20");
        assertThat(c.getCpe()).isEqualTo("cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        assertThat(c.getScope()).isEqualTo(Component.Scope.EXCLUDED);
        assertThat(c.getEvidence()).isNotNull();
        assertThat(c.getEvidence().getIdentities()).isNotNull();
        assertThat(c.getEvidence().getIdentities())
                .anyMatch(id -> id.getField() == Identity.Field.CPE
                        && "cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*".equals(id.getConcludedValue()));
    }

    @Test
    void explicitDescriptionOverridesPomDescription() {
        // A product component uses a Maven (type=pom) PURL, so its effective POM would be resolved.
        // The explicitly configured description (e.g. platform product-description) must win over the
        // description declared in the BOM POM.
        ComponentDescriptor product = ComponentDescriptor.builder()
                .setPurl(Purl.maven("com.redhat.quarkus.platform", "quarkus-bom", "3.33.3.SP1-redhat-00001", "pom", null))
                .setComponentType("framework")
                .setDescription("Red Hat Build of Quarkus")
                .build();

        EffectiveModelResolver resolver = (coords, repos) -> {
            if ("quarkus-bom".equals(coords.getArtifactId())) {
                return pomWithDescription("com.redhat.quarkus.platform", "quarkus-bom",
                        "3.33.3.SP1-redhat-00001", "Quarkus Universe platform aggregates extensions");
            }
            return null;
        };

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setEffectiveModelResolver(resolver)
                .setContributions(List.of(SbomContribution.ofComponents(List.of(product))))
                .generateText().get(0));

        Component c = bom.getComponents().stream()
                .filter(x -> product.getBomRef().equals(x.getBomRef()))
                .findFirst().orElseThrow();
        assertThat(c.getDescription()).isEqualTo("Red Hat Build of Quarkus");
    }

    @Test
    void pomDescriptionUsedWhenDescriptorHasNone() {
        // Without an explicit description, the BOM POM description remains the fallback.
        ComponentDescriptor mavenComp = ComponentDescriptor.builder()
                .setPurl(Purl.maven("org.acme", "acme-lib", "1.0.0", "jar", null))
                .build();

        EffectiveModelResolver resolver = (coords, repos) -> {
            if ("acme-lib".equals(coords.getArtifactId())) {
                return pomWithDescription("org.acme", "acme-lib", "1.0.0", "A handy library");
            }
            return null;
        };

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setEffectiveModelResolver(resolver)
                .setContributions(List.of(SbomContribution.ofComponents(List.of(mavenComp))))
                .generateText().get(0));

        Component c = bom.getComponents().stream()
                .filter(x -> mavenComp.getBomRef().equals(x.getBomRef()))
                .findFirst().orElseThrow();
        assertThat(c.getDescription()).isEqualTo("A handy library");
    }

    @Test
    void explicitLicenseOverridesPomLicense() {
        // The explicitly configured license on a Maven component must win over the license declared
        // in the resolved POM, mirroring the description precedence.
        ComponentDescriptor mavenComp = ComponentDescriptor.builder()
                .setPurl(Purl.maven("org.acme", "acme-lib", "1.0.0", "jar", null))
                .addLicense(new LicenseInfo("MIT"))
                .build();

        EffectiveModelResolver resolver = (coords, repos) -> {
            if ("acme-lib".equals(coords.getArtifactId())) {
                return pomWithLicense("org.acme", "acme-lib", "1.0.0", "Apache-2.0");
            }
            return null;
        };

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setEffectiveModelResolver(resolver)
                .setContributions(List.of(SbomContribution.ofComponents(List.of(mavenComp))))
                .generateText().get(0));

        Component c = bom.getComponents().stream()
                .filter(x -> mavenComp.getBomRef().equals(x.getBomRef()))
                .findFirst().orElseThrow();
        assertThat(c.getLicenses().getLicenses())
                .anyMatch(l -> "MIT".equals(l.getId()) || "MIT".equals(l.getName()))
                .noneMatch(l -> "Apache-2.0".equals(l.getId()) || "Apache-2.0".equals(l.getName()));
    }

    @Test
    void pomLicenseUsedWhenDescriptorHasNone() {
        // Without an explicit license, the POM license remains the fallback.
        ComponentDescriptor mavenComp = ComponentDescriptor.builder()
                .setPurl(Purl.maven("org.acme", "acme-lib", "1.0.0", "jar", null))
                .build();

        EffectiveModelResolver resolver = (coords, repos) -> {
            if ("acme-lib".equals(coords.getArtifactId())) {
                return pomWithLicense("org.acme", "acme-lib", "1.0.0", "Apache-2.0");
            }
            return null;
        };

        Bom bom = parseBom(CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setEffectiveModelResolver(resolver)
                .setContributions(List.of(SbomContribution.ofComponents(List.of(mavenComp))))
                .generateText().get(0));

        Component c = bom.getComponents().stream()
                .filter(x -> mavenComp.getBomRef().equals(x.getBomRef()))
                .findFirst().orElseThrow();
        assertThat(c.getLicenses().getLicenses())
                .anyMatch(l -> "Apache-2.0".equals(l.getId()) || "Apache-2.0".equals(l.getName()));
    }

    private static org.apache.maven.model.Model pomWithDescription(String groupId, String artifactId,
            String version, String description) {
        org.apache.maven.model.Model model = new org.apache.maven.model.Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
        model.setDescription(description);
        return model;
    }

    private static Bom parseBom(String json) {
        try {
            return new JsonParser().parse(json.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BOM JSON", e);
        }
    }

    private static io.quarkus.maven.dependency.ResolvedDependency resolvedDep(
            String groupId, String artifactId, String version,
            List<ArtifactCoords> dependencies) {
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId(groupId)
                .setArtifactId(artifactId)
                .setVersion(version)
                .setResolvedPaths(PathList.of())
                .setDependencies(dependencies)
                .setRuntimeCp()
                .build();
    }

    private static Component findComponent(Bom bom, String name) {
        return bom.getComponents().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Component " + name + " not found"));
    }

    private static boolean hasProperty(Component component, String propertyName) {
        return component.getProperties() != null && component.getProperties().stream()
                .anyMatch(p -> propertyName.equals(p.getName()));
    }

    private static String propertyValue(Component component, String propertyName) {
        if (component.getProperties() == null) {
            return null;
        }
        return component.getProperties().stream()
                .filter(p -> propertyName.equals(p.getName()))
                .map(Property::getValue)
                .findFirst()
                .orElse(null);
    }
}

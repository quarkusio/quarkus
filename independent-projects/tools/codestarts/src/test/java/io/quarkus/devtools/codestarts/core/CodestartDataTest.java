package io.quarkus.devtools.codestarts.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.codestarts.CodestartCatalog;
import io.quarkus.devtools.codestarts.CodestartProjectGenerationTest;
import io.quarkus.devtools.codestarts.CodestartProjectInput;

class CodestartDataTest {
    @Test
    @SuppressWarnings("unchecked")
    void testDependenciesOverrideWithVersion() throws IOException {
        final CodestartCatalog<CodestartProjectInput> catalog = CodestartProjectGenerationTest.loadSpecific("deps");
        final Map<String, Object> dependenciesData = CodestartData.buildDependenciesData(catalog.getCodestarts().stream()
                .filter(c -> c.isSelected(Sets.newLinkedHashSet("codestart-dep1", "codestart-dep2"))),
                "a",
                Lists.list("input.group:some-dep:1.2", "my.group:base-dep1", "my.group:a-dep1:1.1"),
                Lists.list("input.group:some-bom"));
        assertThat((Set<Map<String, String>>) dependenciesData.get("dependencies"))
                .isNotNull()
                .map(m -> m.get("formatted-gav"))
                .containsExactlyInAnyOrder(
                        "input.group:some-dep:1.2",
                        "my.group:base-dep1",
                        "my.group:a-dep1:1.1",
                        "my.group:base-depversion:1.10",
                        "my.group:base-dep2",
                        "my.group:a-dep2");
        assertThat((Set<Map<String, String>>) dependenciesData.get("test-dependencies"))
                .isNotNull()
                .map(m -> m.get("formatted-gav"))
                .containsExactlyInAnyOrder(
                        "my.group:base-test-dep1",
                        "my.group:a-test-dep1",
                        "my.group:base-test-dep2");
        assertThat((Set<Map<String, String>>) dependenciesData.get("boms"))
                .isNotNull()
                .map(m -> m.get("formatted-gav"))
                .containsExactly("input.group:some-bom");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDependenciesOverrideNoVersion() throws IOException {
        final CodestartCatalog<CodestartProjectInput> catalog = CodestartProjectGenerationTest.loadSpecific("deps");
        final Map<String, Object> dependenciesData = CodestartData.buildDependenciesData(catalog.getCodestarts().stream()
                .filter(c -> c.isSelected(Sets.newLinkedHashSet("codestart-dep1"))),
                "a",
                Lists.list("my.group:base-depversion"),
                Lists.newArrayList());
        assertThat((Set<Map<String, String>>) dependenciesData.get("dependencies"))
                .isNotNull()
                .map(m -> m.get("formatted-gav"))
                .containsExactlyInAnyOrder(
                        "my.group:base-dep1",
                        "my.group:a-dep1",
                        "my.group:base-depversion");
    }
}

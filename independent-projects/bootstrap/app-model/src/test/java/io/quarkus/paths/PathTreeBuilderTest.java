package io.quarkus.paths;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PathTreeBuilderTest {

    @Test
    void getIncludesReturnsIncludes() {
        PathTreeBuilder builder = new PathTreeBuilder().include("in*");
        assertThat(builder.getIncludes()).containsExactly("in*");
    }

    @Test
    void getExcludesReturnsExcludes() {
        PathTreeBuilder builder = new PathTreeBuilder().exclude("ex*");
        assertThat(builder.getExcludes()).containsExactly("ex*");
    }

    @Test
    void includesAndExcludesAreIndependent() {
        PathTreeBuilder builder = new PathTreeBuilder().include("in*").exclude("ex*");
        assertThat(builder.getIncludes()).containsExactly("in*");
        assertThat(builder.getExcludes()).containsExactly("ex*");
    }
}

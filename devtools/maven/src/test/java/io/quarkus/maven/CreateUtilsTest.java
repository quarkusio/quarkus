package io.quarkus.maven;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CreateUtilsTest {

    @Test
    void testWithPackage() {
        assertThat(CreateUtils.getDerivedPath("org.acme.ProductResource")).isEqualTo("/product");
    }

    @Test
    void testWithDefaultPackage() {
        assertThat(CreateUtils.getDerivedPath("ProductResource")).isEqualTo("/product");
    }

    @Test
    void testWithoutCamelCase() {
        assertThat(CreateUtils.getDerivedPath("org.acme.Product")).isEqualTo("/product");
    }

    @Test
    void testWithoutCamelCaseAndPackage() {
        assertThat(CreateUtils.getDerivedPath("Product")).isEqualTo("/product");
    }

}

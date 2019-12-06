package io.quarkus.it.jackson.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test cases for InheritedModelWithBuilder.Builder
 */
public class InheritedModelWithBuilderTest {

    // -------------------------------------------------------------------------
    // Test cases
    // -------------------------------------------------------------------------

    @Test
    public void testBuilderMinimal() {
        // prepare
        InheritedModelWithBuilder.Builder builder = new InheritedModelWithBuilder.Builder("device1");

        // execute
        InheritedModelWithBuilder data = builder.build();

        // verify
        assertThat(data.getVersion()).isEqualTo(1);
        assertThat(data.getId()).isEqualTo("device1");
        assertThat(data.getValue()).isEqualTo("");
    }

    @Test
    public void testBuilderUsingOptionals() {
        // prepare
        InheritedModelWithBuilder.Builder builder = new InheritedModelWithBuilder.Builder("device1")
                .withVersion(2)
                .withValue("value");

        // execute
        InheritedModelWithBuilder data = builder.build();

        // verify
        assertThat(data.getVersion()).isEqualTo(2);
        assertThat(data.getId()).isEqualTo("device1");
        assertThat(data.getValue()).isEqualTo("value");
    }

    @Test
    public void testBuilderCloneConstructor() {
        // prepare
        InheritedModelWithBuilder original = new InheritedModelWithBuilder.Builder("device1")
                .withValue("value")
                .build();

        // execute
        InheritedModelWithBuilder clone = new InheritedModelWithBuilder.Builder(original).build();

        // verify
        assertThat(clone.getVersion()).isEqualTo(1);
        assertThat(clone.getId()).isEqualTo("device1");
        assertThat(clone.getValue()).isEqualTo("value");
    }
}

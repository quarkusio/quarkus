package io.quarkus.it.jackson.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test cases for SensorData.Builder
 */
public class ModelWithBuilderTest {

    // -------------------------------------------------------------------------
    // Test cases
    // -------------------------------------------------------------------------

    @Test
    public void testBuilderMinimal() {
        // prepare
        ModelWithBuilder.Builder builder = new ModelWithBuilder.Builder("id1");

        // execute
        ModelWithBuilder data = builder.build();

        // verify
        assertThat(data.getVersion()).isEqualTo(1);
        assertThat(data.getId()).isEqualTo("id1");
        assertThat(data.getValue()).isEqualTo("");
    }

    @Test
    public void testBuilder() {
        // prepare
        ModelWithBuilder.Builder builder = new ModelWithBuilder.Builder("id2")
                .withVersion(2)
                .withValue("value");

        // execute
        ModelWithBuilder data = builder.build();

        // verify
        assertThat(data.getVersion()).isEqualTo(2);
        assertThat(data.getId()).isEqualTo("id2");
        assertThat(data.getValue()).isEqualTo("value");
    }

    @Test
    public void testBuilderCloneConstructor() {
        // prepare
        ModelWithBuilder original = new ModelWithBuilder.Builder("id1")
                .withVersion(3)
                .withValue("val")
                .build();

        // execute
        ModelWithBuilder clone = new ModelWithBuilder.Builder(original).build();

        // verify
        assertThat(clone.getVersion()).isEqualTo(3);
        assertThat(clone.getId()).isEqualTo("id1");
        assertThat(clone.getValue()).isEqualTo("val");
    }
}

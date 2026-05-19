package io.quarkus.sbom;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ComponentDescriptorTest {

    @Test
    void topLevelDefaultsToFalse() {
        ComponentDescriptor descriptor = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .build();
        assertThat(descriptor.isTopLevel()).isFalse();
    }

    @Test
    void topLevelSetToTrue() {
        ComponentDescriptor descriptor = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .setTopLevel(true)
                .build();
        assertThat(descriptor.isTopLevel()).isTrue();
    }

    @Test
    void topLevelPreservedByCopyBuilder() {
        ComponentDescriptor original = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .setTopLevel(true)
                .build();
        ComponentDescriptor copy = new ComponentDescriptor.Builder(original).build();
        assertThat(copy.isTopLevel()).isTrue();
    }
}

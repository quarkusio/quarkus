package io.quarkus.mongodb.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ContextProviderBuildItemTest {

    @Test
    void getContextProviderClassNames() {
        ContextProviderBuildItem item = new ContextProviderBuildItem(List.of("foo.bar"));
        assertThat(item.getContextProviderClassNames())
                .hasSize(1)
                .first()
                .isEqualTo("foo.bar");
    }

    @Test
    void emptyOrNull() {
        ContextProviderBuildItem withNull = new ContextProviderBuildItem(null);
        assertThat(withNull.getContextProviderClassNames()).isEmpty();

        ContextProviderBuildItem empty = new ContextProviderBuildItem(List.of());
        assertThat(empty.getContextProviderClassNames()).isEmpty();
    }
}

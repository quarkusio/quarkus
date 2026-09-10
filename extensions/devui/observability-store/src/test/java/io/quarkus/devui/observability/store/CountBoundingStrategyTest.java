package io.quarkus.devui.observability.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CountBoundingStrategyTest {

    @Test
    void exceedsBoundOnlyAboveMax() {
        CountBoundingStrategy strategy = new CountBoundingStrategy(3);
        assertThat(strategy.maxCount()).isEqualTo(3);
        assertThat(strategy.exceedsBound(3)).isFalse();
        assertThat(strategy.exceedsBound(4)).isTrue();
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new CountBoundingStrategy(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

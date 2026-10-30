package io.quarkus.redis.datasource.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.codecs.Codecs;

class GeoRadiusArgsTest {

    @Test
    void noOrderingTokenWhenNoOrderingIsSet() {
        assertThat(new GeoRadiusArgs().withDistance().count(5).toArgs()).containsExactly("WITHDIST", "COUNT", "5");

        assertThat(new GeoRadiusStoreArgs<String>().storeKey("dest").toArgs(Codecs.StringCodec.INSTANCE))
                .containsExactly("STORE", "dest");
    }

    @Test
    void orderingTokenWhenOrderingIsSet() {
        assertThat(new GeoRadiusArgs().ascending().toArgs()).containsExactly("ASC");
        assertThat(new GeoRadiusArgs().descending().count(5).toArgs()).containsExactly("COUNT", "5", "DESC");

        assertThat(new GeoRadiusStoreArgs<String>().descending().storeDistKey("dist").toArgs(Codecs.StringCodec.INSTANCE))
                .containsExactly("DESC", "STOREDIST", "dist");
    }

    @Test
    void latitudeRangeMessageNamesTheNegativeLowerBound() {
        assertThatThrownBy(() -> GeoPosition.of(0, 90))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The latitude must be in [-85.05112878, 85.05112878]");
    }

}

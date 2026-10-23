package io.quarkus.redis.datasource.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.geo.GeoUnit;

class QueryArgsTest {

    @Test
    void inKeysAndInFieldsEmitTheirKeyword() {
        QueryArgs args = new QueryArgs().inKeys("doc:1", "doc:2").returnAttribute("title").inFields("title");

        assertThat(args.toArgs()).containsExactly("INKEYS", "2", "doc:1", "doc:2", "RETURN", "1", "title", "INFIELDS", "1",
                "title");
    }

    @Test
    void geoFilterEmitsOneTokenPerArgument() {
        QueryArgs args = new QueryArgs().geoFilter(new GeoFilter("location", 2.35, 48.85, 10, GeoUnit.KM));

        assertThat(args.toArgs()).containsExactly("GEOFILTER", "location", "2.35", "48.85", "10.0", "km");
    }

}

package io.quarkus.devservices.postgresql.deployment;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseFeature;
import io.quarkus.devservices.common.ConfigureUtil;

class PostgresImageSelectorTest {

    @Test
    void shouldReturnExplicitImageIfPresent() {
        String image = PostgresImageSelector.selectImage(Optional.of("postgres:latest"),
                Set.of(DatabaseFeature.VECTOR, DatabaseFeature.SPATIAL), "pgsql");
        Assertions.assertEquals("postgres:latest", image);
    }

    @Test
    void shouldReturnDefaultImageIfNoFeatures() {
        String image = PostgresImageSelector.selectImage(Optional.empty(), Set.of(), "pgsql");
        Assertions.assertEquals(ConfigureUtil.getDefaultImageNameFor("postgresql"), image);
    }

    @Test
    void shouldReturnDefaultImageIfCombinationOfFeatures() {
        String image = PostgresImageSelector.selectImage(Optional.empty(),
                Set.of(DatabaseFeature.VECTOR, DatabaseFeature.SPATIAL), "pgsql");
        Assertions.assertEquals(ConfigureUtil.getDefaultImageNameFor("postgresql"), image);
    }

    @Test
    void shouldReturnVectorImageIfVectorFeatureIsPresent() {
        String image = PostgresImageSelector.selectImage(Optional.empty(), Set.of(DatabaseFeature.VECTOR), "pgsql");
        Assertions.assertEquals(ConfigureUtil.getDefaultImageNameFor("pgvector"), image);
    }

    @Test
    void shouldReturnSpatialImageIfSpatialFeatureIsPresent() {
        String image = PostgresImageSelector.selectImage(Optional.empty(), Set.of(DatabaseFeature.SPATIAL), "pgsql");
        Assertions.assertEquals(ConfigureUtil.getDefaultImageNameFor("postgis"), image);
    }
}

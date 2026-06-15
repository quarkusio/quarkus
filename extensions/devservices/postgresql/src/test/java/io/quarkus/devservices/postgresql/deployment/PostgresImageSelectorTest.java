package io.quarkus.devservices.postgresql.deployment;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseFeature;
import io.quarkus.devservices.common.ConfigureUtil;
import io.smallrye.common.cpu.CPU;

class PostgresImageSelectorTest {

    private static final CPU DEFAULT_ARCH = CPU.x64;

    @Test
    void shouldReturnExplicitImageIfPresent() {
        String image = PostgresImageSelector.selectImage(Optional.of("postgres:latest"),
                Set.of(DatabaseFeature.VECTOR, DatabaseFeature.SPATIAL), "pgsql", DEFAULT_ARCH);
        Assertions.assertEquals("postgres:latest", image);
    }

    @Test
    void shouldReturnDefaultImageIfNoFeatures() {
        String image = PostgresImageSelector.selectImage(Optional.empty(), Set.of(), "pgsql", DEFAULT_ARCH);
        Assertions.assertEquals(ConfigureUtil.getDefaultImageNameFor("postgresql"), image);
    }

    @Test
    void shouldReturnDefaultImageIfCombinationOfFeatures() {
        String image = PostgresImageSelector.selectImage(Optional.empty(),
                Set.of(DatabaseFeature.VECTOR, DatabaseFeature.SPATIAL), "pgsql", DEFAULT_ARCH);
        Assertions.assertEquals(ConfigureUtil.getDefaultImageNameFor("postgresql"), image);
    }

    @Test
    void shouldReturnVectorImageIfVectorFeatureIsPresent() {
        String image = PostgresImageSelector.selectImage(Optional.empty(), Set.of(DatabaseFeature.VECTOR), "pgsql",
                DEFAULT_ARCH);
        Assertions.assertEquals(ConfigureUtil.getDefaultImageNameFor("pgvector"), image);
    }

    @Test
    void shouldReturnSpatialImageIfSpatialFeatureIsPresentOnSupportedArch() {
        String image = PostgresImageSelector.selectImage(Optional.empty(), Set.of(DatabaseFeature.SPATIAL), "pgsql",
                DEFAULT_ARCH);
        Assertions.assertEquals(ConfigureUtil.getDefaultImageNameFor("postgis"), image);
    }

    @Test
    void shouldFallBackToDefaultImageIfSpatialFeatureOnUnsupportedArch() {
        String image = PostgresImageSelector.selectImage(Optional.empty(), Set.of(DatabaseFeature.SPATIAL), "pgsql",
                CPU.aarch64);
        Assertions.assertEquals(ConfigureUtil.getDefaultImageNameFor("postgresql"), image);
    }
}

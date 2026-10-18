package io.quarkus.devservices.postgresql.deployment;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DatabaseFeature;
import io.quarkus.devservices.common.ConfigureUtil;
import io.smallrye.common.cpu.CPU;

class PostgresImageSelector {

    private static final Logger log = Logger.getLogger(PostgresImageSelector.class);

    static final Set<CPU> POSTGIS_SUPPORTED_ARCHITECTURES = Set.of(CPU.x64);

    static String selectImage(Optional<String> explicitImage, Set<DatabaseFeature> requiredFeatures,
            String datasourceName, CPU architecture) {
        if (explicitImage.isPresent()) {
            return explicitImage.get();
        }

        if (requiredFeatures.isEmpty()) {
            return ConfigureUtil.getDefaultImageNameFor("postgresql");
        }

        if (requiredFeatures.size() == 1) {
            DatabaseFeature feature = requiredFeatures.iterator().next();
            return switch (feature) {
                case VECTOR -> ConfigureUtil.getDefaultImageNameFor("pgvector");
                case SPATIAL -> selectPostgisOrFallback(datasourceName, architecture);
            };
        }

        String featureNames = requiredFeatures.stream()
                .map(f -> f.name().toLowerCase())
                .collect(Collectors.joining(", "));

        log.warnv("No known PostgreSQL image supports the combination of features: {0}. "
                + "Please specify an explicit image using '{1}' "
                + "that supports these features.",
                featureNames,
                DataSourceUtil.dataSourcePropertyKey(datasourceName, "devservices.image-name"));

        return ConfigureUtil.getDefaultImageNameFor("postgresql");
    }

    private static String selectPostgisOrFallback(String datasourceName, CPU architecture) {
        if (POSTGIS_SUPPORTED_ARCHITECTURES.contains(architecture)) {
            return ConfigureUtil.getDefaultImageNameFor("postgis");
        }
        log.warnv("The PostGIS container image is not available for architecture '{0}'."
                + " Falling back to a plain PostgreSQL image without PostGIS support."
                + " To use PostGIS on this architecture, specify a compatible image using '{1}'.",
                architecture,
                DataSourceUtil.dataSourcePropertyKey(datasourceName, "devservices.image-name"));
        return ConfigureUtil.getDefaultImageNameFor("postgresql");
    }
}

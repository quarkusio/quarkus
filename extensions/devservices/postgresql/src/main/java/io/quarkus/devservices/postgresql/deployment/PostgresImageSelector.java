package io.quarkus.devservices.postgresql.deployment;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DatabaseFeature;
import io.quarkus.devservices.common.ConfigureUtil;

class PostgresImageSelector {

    private static final Logger log = Logger.getLogger(PostgresImageSelector.class);

    static String selectImage(Optional<String> explicitImage, Set<DatabaseFeature> requiredFeatures, String datasourceName) {
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
                case SPATIAL -> ConfigureUtil.getDefaultImageNameFor("postgis");
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
}

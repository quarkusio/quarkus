package io.quarkus.datasource.deployment.spi;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Helper for Agroal/Reactive datasource extensions to resolve the db-kind of a datasource
 * and produce appropriate error messages if necessary.
 */
public final class DataSourceDbKindResolverBuildItem extends SimpleBuildItem {
    private final DbKindResolver resolver;

    public DataSourceDbKindResolverBuildItem(DbKindResolver resolver) {
        this.resolver = resolver;
    }

    public DbKindResolver get() {
        return resolver;
    }

    public interface DbKindResolver {

        Optional<String> getOptional(String dataSourceName);

        Reason unavailableReason(String dataSourceName, ProgrammingParadigm paradigm);

    }
}

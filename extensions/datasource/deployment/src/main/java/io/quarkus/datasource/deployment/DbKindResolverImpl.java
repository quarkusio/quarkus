package io.quarkus.datasource.deployment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DataSourceDbKindResolverBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

public abstract class DbKindResolverImpl implements DataSourceDbKindResolverBuildItem.DbKindResolver {
    private final DataSourcesBuildTimeConfig config;

    private DbKindResolverImpl(DataSourcesBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public Optional<String> getOptional(String dataSourceName) {
        return config.dataSources().get(dataSourceName).dbKind();
    }

    public static class NoDefault extends DbKindResolverImpl {
        public NoDefault(DataSourcesBuildTimeConfig config) {
            super(config);
        }

        @Override
        public Reason unavailableReason(String dataSourceName, ProgrammingParadigm paradigm) {
            return new Reason(switch (paradigm) {
                case BLOCKING -> String.format(Locale.ROOT, """
                        Cannot infer the database kind from the classpath, because no JDBC driver extension is available. \
                        Add a JDBC driver extension (e.g. quarkus-jdbc-postgresql, quarkus-jdbc-mysql, etc.), \
                        or set '%s=other' and configure the JDBC driver explicitly. \
                        Refer to https://quarkus.io/guides/datasource for guidance.
                        """,
                        DataSourceUtil.dataSourcePropertyKey(dataSourceName, "db-kind"));
                case REACTIVE ->
                    """
                            Cannot infer the database kind from the classpath, because no reactive SQL client extension is available. \
                            Add a Reactive SQL Client extension \
                            (e.g. quarkus-reactive-pg-client, quarkus-reactive-mysql-client, etc.). \
                            Refer to https://quarkus.io/guides/datasource for guidance.
                            """;
            });
        }
    }

    public static class SingleDefault extends DbKindResolverImpl {
        private final String theDefault;

        public SingleDefault(DataSourcesBuildTimeConfig config, String theDefault) {
            super(config);
            this.theDefault = theDefault;
        }

        @Override
        public Optional<String> getOptional(String dataSourceName) {
            var result = super.getOptional(dataSourceName);
            if (result.isEmpty()) {
                result = Optional.of(theDefault);
            }
            return result;
        }

        @Override
        public Reason unavailableReason(String dataSourceName, ProgrammingParadigm paradigm) {
            throw new IllegalStateException("This method should not be called, because the db-kind _is_ available");
        }
    }

    public static class MultipleDefaults extends DbKindResolverImpl {
        private final List<String> defaultDbKindStrings;

        public MultipleDefaults(DataSourcesBuildTimeConfig config, List<String> defaultDbKindStrings) {
            super(config);
            this.defaultDbKindStrings = defaultDbKindStrings;
        }

        @Override
        public Optional<String> getOptional(String dataSourceName) {
            // If db-kind is explicitly configured, use it; otherwise we can't pick a default
            return super.getOptional(dataSourceName);
        }

        @Override
        public Reason unavailableReason(String dataSourceName, ProgrammingParadigm paradigm) {
            String dbKindProperty = DataSourceUtil.dataSourcePropertyKey(dataSourceName, "db-kind");
            return new Reason(switch (paradigm) {
                case BLOCKING -> String.format(Locale.ROOT,
                        """
                                Cannot infer the database kind from the classpath, because multiple JDBC driver extensions are available: %s. \
                                Select an extension by setting '%s' to one of these values, \
                                or set '%s=other' and configure the JDBC driver explicitly. \
                                Refer to https://quarkus.io/guides/datasource for guidance.
                                """,
                        defaultDbKindStrings, dbKindProperty, dbKindProperty);
                case REACTIVE -> String.format(Locale.ROOT,
                        """
                                Cannot infer the database kind from the classpath, because multiple reactive SQL client extensions are available: %s. \
                                Select an extension by setting '%s' to one of these values. \
                                Refer to https://quarkus.io/guides/datasource for guidance.
                                """,
                        defaultDbKindStrings, dbKindProperty);
            });
        }
    }
}

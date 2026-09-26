package io.quarkus.hibernate.orm.dev;

import java.util.Properties;

import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.spi.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.spi.insert.SqmInsertStatement;

public class H2CustomDialect extends H2Dialect {

    @Override
    protected void contributeDefaultProperties(Properties properties) {
        super.contributeDefaultProperties(properties);
        // Avoid resolving the deliberately invalid SQL type during temporary-table setup.
        properties.setProperty(QuerySettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY, NoOpMutationStrategy.class.getName());
        properties.setProperty(QuerySettings.QUERY_MULTI_TABLE_INSERT_STRATEGY, NoOpInsertStrategy.class.getName());
    }

    public static class NoOpMutationStrategy implements SqmMultiTableMutationStrategy {
        @Override
        public MultiTableHandlerBuildResult buildHandler(SqmDeleteOrUpdateStatement<?> sqmStatement,
                DomainParameterXref domainParameterXref, DomainQueryExecutionContext context) {
            return null;
        }
    }

    public static class NoOpInsertStrategy implements SqmMultiTableInsertStrategy {
        @Override
        public MultiTableHandlerBuildResult buildHandler(SqmInsertStatement<?> sqmInsertStatement,
                DomainParameterXref domainParameterXref, DomainQueryExecutionContext context) {
            return null;
        }
    }
}

package io.quarkus.hibernate.orm.dev;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.spi.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.spi.insert.SqmInsertStatement;

public class H2CustomDialect extends H2Dialect {

    @Override
    public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
            EntityMappingType entityDescriptor,
            RuntimeModelCreationContext runtimeModelCreationContext) {
        return new SqmMultiTableMutationStrategy() {
            @Override
            public MultiTableHandlerBuildResult buildHandler(SqmDeleteOrUpdateStatement<?> sqmDeleteOrUpdateStatement,
                    DomainParameterXref domainParameterXref, DomainQueryExecutionContext domainQueryExecutionContext) {
                return null;
            }
        };
    }

    @Override
    public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
            EntityMappingType entityDescriptor,
            RuntimeModelCreationContext runtimeModelCreationContext) {
        return new SqmMultiTableInsertStrategy() {
            @Override
            public MultiTableHandlerBuildResult buildHandler(SqmInsertStatement<?> sqmInsertStatement,
                    DomainParameterXref domainParameterXref, DomainQueryExecutionContext context) {
                return null;
            }
        };
    }
}

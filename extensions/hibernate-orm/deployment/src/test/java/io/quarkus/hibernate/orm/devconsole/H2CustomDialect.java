package io.quarkus.hibernate.orm.devconsole;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

public class H2CustomDialect extends H2Dialect {

    @Override
    public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
            EntityMappingType entityDescriptor,
            RuntimeModelCreationContext runtimeModelCreationContext) {
        return new SqmMultiTableMutationStrategy() {
            @Override
            public int executeUpdate(
                    SqmUpdateStatement<?> sqmUpdateStatement,
                    DomainParameterXref domainParameterXref,
                    DomainQueryExecutionContext domainQueryExecutionContext) {
                return 0;
            }

            @Override
            public int executeDelete(
                    SqmDeleteStatement<?> sqmDeleteStatement,
                    DomainParameterXref domainParameterXref,
                    DomainQueryExecutionContext domainQueryExecutionContext) {
                return 0;
            }
        };
    }

    @Override
    public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
            EntityMappingType entityDescriptor,
            RuntimeModelCreationContext runtimeModelCreationContext) {
        return new SqmMultiTableInsertStrategy() {
            @Override
            public int executeInsert(
                    SqmInsertStatement<?> sqmInsertStatement,
                    DomainParameterXref domainParameterXref,
                    DomainQueryExecutionContext domainQueryExecutionContext) {
                return 0;
            }
        };
    }
}

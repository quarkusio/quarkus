package io.quarkus.it.jpa.defaultcatalogandschema;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.relational.AbstractAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.MetadataBuilderContributor;

public class Schema1MetadataBuilderContributor implements MetadataBuilderContributor {
    @Override
    public void contribute(MetadataBuilder metadataBuilder) {
        metadataBuilder.applyAuxiliaryDatabaseObject(new AbstractAuxiliaryDatabaseObject(true) {
            @Override
            public String[] sqlCreateStrings(SqlStringGenerationContext context) {
                return new String[] { "create schema \"SCHEMA1\"" };
            }

            @Override
            public String[] sqlDropStrings(SqlStringGenerationContext context) {
                return new String[0];
            }
        });
    }
}

package io.quarkus.it.jpa.defaultcatalogandschema;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.relational.AbstractAuxiliaryDatabaseObject;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.Dialect;

public class Schema1MetadataBuilderContributor implements MetadataBuilderContributor {
    @Override
    public void contribute(MetadataBuilder metadataBuilder) {
        metadataBuilder.applyAuxiliaryDatabaseObject(new AbstractAuxiliaryDatabaseObject(true) {
            @Override
            public String[] sqlCreateStrings(Dialect dialect) {
                return new String[] { "create schema \"SCHEMA1\"" };
            }

            @Override
            public String[] sqlDropStrings(Dialect dialect) {
                return new String[0];
            }
        });
    }
}

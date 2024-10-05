package io.quarkus.liquibase.snapshot;

import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Catalog;
import liquibase.structure.core.Schema;

/**
 * Hibernate doesn't really support Schemas, so just return the passed example back as if it had all the info it needed.
 */
public class SchemaSnapshotGenerator extends HibernateSnapshotGenerator {

    public SchemaSnapshotGenerator() {
        super(Schema.class, new Class[] { Catalog.class });
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        return new Schema(snapshot.getDatabase().getDefaultCatalogName(), snapshot.getDatabase().getDefaultSchemaName())
                .setDefault(true);
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        // Nothing to do
    }

}

package io.quarkus.liquibase.snapshot;

import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Catalog;

/**
 * Hibernate doesn't really support Catalogs, so just return the passed example back as if it had all the info it needed.
 */
public class CatalogSnapshotGenerator extends HibernateSnapshotGenerator {

    public CatalogSnapshotGenerator() {
        super(Catalog.class);
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        return new Catalog(snapshot.getDatabase().getDefaultCatalogName()).setDefault(true);
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        // Nothing to add to
    }

}

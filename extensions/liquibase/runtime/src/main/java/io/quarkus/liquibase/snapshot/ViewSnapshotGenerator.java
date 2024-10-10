package io.quarkus.liquibase.snapshot;

import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;
import liquibase.structure.core.View;

/**
 * View snapshots are not supported from hibernate, but this class needs to be implemented in order to prevent the default
 * ViewSnapshotGenerator from running.
 */
public class ViewSnapshotGenerator extends HibernateSnapshotGenerator {

    public ViewSnapshotGenerator() {
        super(View.class, new Class[] { Schema.class });
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        throw new DatabaseException("No views in Hibernate mapping");
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        // No views in Hibernate mapping

    }

}

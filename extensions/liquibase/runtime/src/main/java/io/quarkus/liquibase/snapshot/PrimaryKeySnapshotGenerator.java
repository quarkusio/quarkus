package io.quarkus.liquibase.snapshot;

import org.hibernate.sql.Alias;

import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.Index;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Table;

public class PrimaryKeySnapshotGenerator extends HibernateSnapshotGenerator {

    private static final int PK_NAME_LENGTH = 63;
    private static final String PK_NAME_SUFFIX = "PK";
    private static final Alias PK_NAME_ALIAS = new Alias(PK_NAME_LENGTH, PK_NAME_SUFFIX);

    public PrimaryKeySnapshotGenerator() {
        super(PrimaryKey.class, new Class[] { Table.class });
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        return example;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(PrimaryKey.class)) {
            return;
        }
        if (foundObject instanceof Table) {
            Table table = (Table) foundObject;
            org.hibernate.mapping.Table hibernateTable = findHibernateTable(table, snapshot);
            if (hibernateTable == null) {
                return;
            }
            org.hibernate.mapping.PrimaryKey hibernatePrimaryKey = hibernateTable.getPrimaryKey();
            if (hibernatePrimaryKey != null) {
                PrimaryKey pk = new PrimaryKey();
                String hbnTableName = hibernateTable.getName();

                String pkName = PK_NAME_ALIAS.toAliasString(hbnTableName);
                if (pkName.length() == PK_NAME_LENGTH) {
                    String suffix = "_" + Integer.toHexString(hbnTableName.hashCode()).toUpperCase() + "_" + PK_NAME_SUFFIX;
                    pkName = pkName.substring(0, PK_NAME_LENGTH - suffix.length()) + suffix;
                }
                pk.setName(pkName);

                pk.setTable(table);
                for (org.hibernate.mapping.Column hibernateColumn : hibernatePrimaryKey.getColumns()) {
                    pk.getColumns().add(new Column(hibernateColumn.getName()).setRelation(table));
                }

                Scope.getCurrentScope().getLog(getClass()).info("Found primary key " + pk.getName());
                table.setPrimaryKey(pk);
                Index index = new Index();
                index.setName("IX_" + pk.getName());
                index.setRelation(table);
                index.setColumns(pk.getColumns());
                index.setUnique(true);
                pk.setBackingIndex(index);
                table.getIndexes().add(index);
            }
        }
    }

}

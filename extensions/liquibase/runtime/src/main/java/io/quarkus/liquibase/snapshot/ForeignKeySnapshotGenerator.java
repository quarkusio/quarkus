package io.quarkus.liquibase.snapshot;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.boot.spi.MetadataImplementor;

import io.quarkus.liquibase.database.QuarkusDatabase;
import liquibase.diff.compare.DatabaseObjectComparatorFactory;
import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.ForeignKey;
import liquibase.structure.core.Table;

public class ForeignKeySnapshotGenerator extends HibernateSnapshotGenerator {

    public ForeignKeySnapshotGenerator() {
        super(ForeignKey.class, new Class[] { Table.class });
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        return example;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(ForeignKey.class)) {
            return;
        }
        if (foundObject instanceof Table) {
            Table table = (Table) foundObject;
            QuarkusDatabase database = (QuarkusDatabase) snapshot.getDatabase();
            MetadataImplementor metadata = (MetadataImplementor) database.getMetadata();

            Collection<org.hibernate.mapping.Table> tmapp = metadata.collectTableMappings();
            Iterator<org.hibernate.mapping.Table> tableMappings = tmapp.iterator();
            while (tableMappings.hasNext()) {
                org.hibernate.mapping.Table hibernateTable = (org.hibernate.mapping.Table) tableMappings.next();
                Iterator fkIterator = hibernateTable.getForeignKeyIterator();
                while (fkIterator.hasNext()) {
                    org.hibernate.mapping.ForeignKey hibernateForeignKey = (org.hibernate.mapping.ForeignKey) fkIterator.next();
                    Table currentTable = new Table().setName(hibernateTable.getName());
                    currentTable.setSchema(hibernateTable.getCatalog(), hibernateTable.getSchema());

                    org.hibernate.mapping.Table hibernateReferencedTable = hibernateForeignKey.getReferencedTable();
                    Table referencedTable = new Table().setName(hibernateReferencedTable.getName());
                    referencedTable.setSchema(hibernateReferencedTable.getCatalog(), hibernateReferencedTable.getSchema());

                    if (hibernateForeignKey.isCreationEnabled() && hibernateForeignKey.isPhysicalConstraint()) {
                        ForeignKey fk = new ForeignKey();
                        fk.setName(hibernateForeignKey.getName());
                        fk.setPrimaryKeyTable(referencedTable);
                        fk.setForeignKeyTable(currentTable);
                        for (Object column : hibernateForeignKey.getColumns()) {
                            fk.addForeignKeyColumn(
                                    new liquibase.structure.core.Column(((org.hibernate.mapping.Column) column).getName()));
                        }
                        for (Object column : hibernateForeignKey.getReferencedColumns()) {
                            fk.addPrimaryKeyColumn(
                                    new liquibase.structure.core.Column(((org.hibernate.mapping.Column) column).getName()));
                        }
                        if (fk.getPrimaryKeyColumns() == null || fk.getPrimaryKeyColumns().isEmpty()) {
                            for (Object column : hibernateReferencedTable.getPrimaryKey().getColumns()) {
                                fk.addPrimaryKeyColumn(
                                        new liquibase.structure.core.Column(((org.hibernate.mapping.Column) column).getName()));
                            }
                        }

                        fk.setDeferrable(false);
                        fk.setInitiallyDeferred(false);

                        //			Index index = new Index();
                        //			index.setName("IX_" + fk.getName());
                        //			index.setTable(fk.getForeignKeyTable());
                        //			index.setColumns(fk.getForeignKeyColumns());
                        //			fk.setBackingIndex(index);
                        //			table.getIndexes().add(index);

                        if (DatabaseObjectComparatorFactory.getInstance().isSameObject(currentTable, table, null, database)) {
                            table.getOutgoingForeignKeys().add(fk);
                            table.getSchema().addDatabaseObject(fk);
                        }
                    }
                }
            }
        }
    }

}

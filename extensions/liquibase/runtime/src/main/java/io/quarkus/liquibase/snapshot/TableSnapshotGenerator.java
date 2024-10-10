package io.quarkus.liquibase.snapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;

import io.quarkus.liquibase.database.QuarkusDatabase;
import io.quarkus.liquibase.snapshot.extension.ExtendedSnapshotGenerator;
import io.quarkus.liquibase.snapshot.extension.TableGeneratorSnapshotGenerator;
import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;

public class TableSnapshotGenerator extends HibernateSnapshotGenerator {

    private List<ExtendedSnapshotGenerator<Generator, Table>> tableIdGenerators = new ArrayList<>();

    public TableSnapshotGenerator() {
        super(Table.class, new Class[] { Schema.class });
        tableIdGenerators.add(new TableGeneratorSnapshotGenerator());
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        if (example.getSnapshotId() != null) {
            return example;
        }
        org.hibernate.mapping.Table hibernateTable = findHibernateTable(example, snapshot);
        if (hibernateTable == null) {
            return example;
        }

        Table table = new Table().setName(hibernateTable.getName());
        Scope.getCurrentScope().getLog(getClass()).info("Found table " + table.getName());
        table.setSchema(example.getSchema());
        if (hibernateTable.getComment() != null && !hibernateTable.getComment().isEmpty()) {
            table.setRemarks(hibernateTable.getComment());
        }

        return table;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(Table.class)) {
            return;
        }

        if (foundObject instanceof Schema) {

            Schema schema = (Schema) foundObject;
            QuarkusDatabase database = (QuarkusDatabase) snapshot.getDatabase();
            MetadataImplementor metadata = (MetadataImplementor) database.getMetadata();

            Collection<PersistentClass> entityBindings = metadata.getEntityBindings();
            Iterator<PersistentClass> tableMappings = entityBindings.iterator();

            while (tableMappings.hasNext()) {
                PersistentClass pc = tableMappings.next();

                org.hibernate.mapping.Table hibernateTable = pc.getTable();
                if (hibernateTable.isPhysicalTable()) {
                    addDatabaseObjectToSchema(hibernateTable, schema, snapshot);

                    Collection<Join> joins = pc.getJoins();
                    Iterator<Join> joinMappings = joins.iterator();
                    while (joinMappings.hasNext()) {
                        Join join = joinMappings.next();
                        addDatabaseObjectToSchema(join.getTable(), schema, snapshot);
                    }
                }
            }

            Iterator<PersistentClass> classMappings = entityBindings.iterator();
            while (classMappings.hasNext()) {
                PersistentClass persistentClass = classMappings.next();
                if (!persistentClass.isInherited() && persistentClass.getIdentifier() instanceof SimpleValue) {
                    var simpleValue = (SimpleValue) persistentClass.getIdentifier();
                    Generator ig = simpleValue.createGenerator(
                            metadata.getMetadataBuildingOptions().getIdentifierGeneratorFactory(),
                            database.getDialect(),
                            (RootClass) persistentClass);
                    for (ExtendedSnapshotGenerator<Generator, Table> tableIdGenerator : tableIdGenerators) {
                        if (tableIdGenerator.supports(ig)) {
                            Table idTable = tableIdGenerator.snapshot(ig);
                            idTable.setSchema(schema);
                            schema.addDatabaseObject(snapshotObject(idTable, snapshot));
                            break;
                        }
                    }
                }
            }

            Collection<org.hibernate.mapping.Collection> collectionBindings = metadata.getCollectionBindings();
            Iterator<org.hibernate.mapping.Collection> collIter = collectionBindings.iterator();
            while (collIter.hasNext()) {
                org.hibernate.mapping.Collection coll = collIter.next();
                org.hibernate.mapping.Table hTable = coll.getCollectionTable();
                if (hTable.isPhysicalTable()) {
                    addDatabaseObjectToSchema(hTable, schema, snapshot);
                }
            }
        }
    }

    private void addDatabaseObjectToSchema(org.hibernate.mapping.Table join, Schema schema, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        Table joinTable = new Table().setName(join.getName());
        joinTable.setSchema(schema);
        Scope.getCurrentScope().getLog(getClass()).info("Found table " + joinTable.getName());
        schema.addDatabaseObject(snapshotObject(joinTable, snapshot));
    }
}

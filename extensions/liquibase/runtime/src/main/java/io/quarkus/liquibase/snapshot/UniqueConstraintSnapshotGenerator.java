package io.quarkus.liquibase.snapshot;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.hibernate.HibernateException;

import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.Index;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;
import liquibase.util.StringUtil;

public class UniqueConstraintSnapshotGenerator extends HibernateSnapshotGenerator {

    public UniqueConstraintSnapshotGenerator() {
        super(UniqueConstraint.class, new Class[] { Table.class });
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        return example;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot)
            throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(UniqueConstraint.class)) {
            return;
        }

        if (foundObject instanceof Table) {
            Table table = (Table) foundObject;
            org.hibernate.mapping.Table hibernateTable = findHibernateTable(table, snapshot);
            if (hibernateTable == null) {
                return;
            }
            for (var hibernateUnique : hibernateTable.getUniqueKeys().values()) {
                UniqueConstraint uniqueConstraint = new UniqueConstraint();
                uniqueConstraint.setName(hibernateUnique.getName());
                uniqueConstraint.setRelation(table);
                uniqueConstraint.setClustered(false); // No way to set true via Hibernate

                int i = 0;
                for (var hibernateColumn : hibernateUnique.getColumns()) {
                    uniqueConstraint.addColumn(i++, new Column(hibernateColumn.getName()).setRelation(table));
                }

                Index index = getBackingIndex(uniqueConstraint, hibernateTable, snapshot);
                uniqueConstraint.setBackingIndex(index);

                Scope.getCurrentScope().getLog(getClass()).info("Found unique constraint " + uniqueConstraint);
                table.getUniqueConstraints().add(uniqueConstraint);
            }
            for (var column : hibernateTable.getColumns()) {
                if (column.isUnique()) {
                    UniqueConstraint uniqueConstraint = new UniqueConstraint();
                    uniqueConstraint.setRelation(table);
                    uniqueConstraint.setClustered(false); // No way to set true via Hibernate
                    String name = "UC_" + table.getName().toUpperCase() + column.getName().toUpperCase() + "_COL";
                    if (name.length() > 64) {
                        name = name.substring(0, 63);
                    }
                    uniqueConstraint.addColumn(0, new Column(column.getName()).setRelation(table));
                    uniqueConstraint.setName(name);
                    Scope.getCurrentScope().getLog(getClass()).info("Found unique constraint " + uniqueConstraint);
                    table.getUniqueConstraints().add(uniqueConstraint);

                    Index index = getBackingIndex(uniqueConstraint, hibernateTable, snapshot);
                    uniqueConstraint.setBackingIndex(index);

                }
            }

            for (UniqueConstraint uc : table.getUniqueConstraints()) {
                if (uc.getName() == null || uc.getName().isEmpty()) {
                    String name = table.getName() + uc.getColumnNames();
                    name = "UCIDX" + hashedName(name);
                    uc.setName(name);
                }
            }
        }
    }

    private String hashedName(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(s.getBytes());
            byte[] digest = md.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            // By converting to base 35 (full alphanumeric), we guarantee
            // that the length of the name will always be smaller than the 30
            // character identifier restriction enforced by a few dialects.
            return bigInt.toString(35);
        } catch (NoSuchAlgorithmException e) {
            throw new HibernateException("Unable to generate a hashed name!", e);
        }
    }

    protected Index getBackingIndex(UniqueConstraint uniqueConstraint, org.hibernate.mapping.Table hibernateTable,
            DatabaseSnapshot snapshot) {
        Index index = new Index();
        index.setRelation(uniqueConstraint.getRelation());
        index.setColumns(uniqueConstraint.getColumns());
        index.setUnique(true);
        index.setName(String.format("%s_%s_IX", hibernateTable.getName(), StringUtil.randomIdentifer(4)));

        return index;
    }

}

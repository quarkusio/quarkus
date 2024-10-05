package io.quarkus.liquibase.diff;

import io.quarkus.liquibase.database.QuarkusDatabase;
import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.ForeignKey;

/**
 * Hibernate doesn't know about all the variations that occur with foreign keys but just whether the FK exists or not.
 * To prevent changing customized foreign keys, we suppress all foreign key changes from hibernate.
 */
public class ChangedForeignKeyChangeGenerator extends liquibase.diff.output.changelog.core.ChangedForeignKeyChangeGenerator {

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (ForeignKey.class.isAssignableFrom(objectType)) {
            return PRIORITY_ADDITIONAL;
        }
        return PRIORITY_NONE;
    }

    @Override
    public Change[] fixChanged(DatabaseObject changedObject, ObjectDifferences differences, DiffOutputControl control,
            Database referenceDatabase, Database comparisonDatabase, ChangeGeneratorChain chain) {
        if (referenceDatabase instanceof QuarkusDatabase || comparisonDatabase instanceof QuarkusDatabase) {
            differences.removeDifference("deleteRule");
            differences.removeDifference("updateRule");
            if (!differences.hasDifferences()) {
                return null;
            }
        }

        return super.fixChanged(changedObject, differences, control, referenceDatabase, comparisonDatabase, chain);
    }
}

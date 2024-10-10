package io.quarkus.liquibase.diff;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.liquibase.database.QuarkusDatabase;
import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Sequence;

/**
 * Hibernate manages sequences only by the name, startValue and incrementBy fields.
 * However, non-hibernate databases might return default values for other fields triggering false positives.
 */
public class ChangedSequenceChangeGenerator extends liquibase.diff.output.changelog.core.ChangedSequenceChangeGenerator {

    private static final Set<String> HIBERNATE_SEQUENCE_FIELDS;

    static {
        HashSet<String> hibernateSequenceFields = new HashSet<>();
        hibernateSequenceFields.add("name");
        hibernateSequenceFields.add("startValue");
        hibernateSequenceFields.add("incrementBy");
        HIBERNATE_SEQUENCE_FIELDS = Collections.unmodifiableSet(hibernateSequenceFields);
    }

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (Sequence.class.isAssignableFrom(objectType)) {
            return PRIORITY_ADDITIONAL;
        }
        return PRIORITY_NONE;
    }

    @Override
    public Change[] fixChanged(DatabaseObject changedObject, ObjectDifferences differences, DiffOutputControl control,
            Database referenceDatabase, Database comparisonDatabase, ChangeGeneratorChain chain) {
        if (!(referenceDatabase instanceof QuarkusDatabase || comparisonDatabase instanceof QuarkusDatabase)) {
            return super.fixChanged(changedObject, differences, control, referenceDatabase, comparisonDatabase, chain);
        }

        // if any of the databases is a hibernate database, remove all differences that affect a field not managed by hibernate
        Set<String> ignoredDifferenceFields = differences.getDifferences().stream()
                .map(Difference::getField)
                .filter(differenceField -> !HIBERNATE_SEQUENCE_FIELDS.contains(differenceField))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        ignoredDifferenceFields.forEach(differences::removeDifference);
        this.advancedIgnoredDifferenceFields(differences, referenceDatabase, comparisonDatabase);
        return super.fixChanged(changedObject, differences, control, referenceDatabase, comparisonDatabase, chain);
    }

    /**
     * In some cases a value that was 1 can be null in the database, or the name field can be different only by case.
     * This method removes these differences from the list of differences so we don't generate a change for them.
     */
    private void advancedIgnoredDifferenceFields(ObjectDifferences differences, Database referenceDatabase,
            Database comparisonDatabase) {
        Set<String> ignoredDifferenceFields = new HashSet<>();
        for (Difference difference : differences.getDifferences()) {
            String field = difference.getField();
            String refValue = difference.getReferenceValue() != null ? difference.getReferenceValue().toString() : null;
            String comparedValue = difference.getComparedValue() != null ? difference.getComparedValue().toString() : null;

            // if the name field case is different and the databases are case-insensitive, we can ignore the difference
            boolean isNameField = field.equals("name");
            boolean isCaseInsensitive = !referenceDatabase.isCaseSensitive() || !comparisonDatabase.isCaseSensitive();

            // if the startValue or incrementBy fields are 1 and the other is null, we can ignore the difference
            // Or 50, as it is the default value for hibernate for allocationSize:
            // https://github.com/hibernate/hibernate-orm/blob/bda95dfbe75c68f5c1b77a2f21c403cbe08548a2/hibernate-core/src/main/java/org/hibernate/boot/model/IdentifierGeneratorDefinition.java#L252
            boolean isStartOrIncrementField = field.equals("startValue") || field.equals("incrementBy");
            boolean isOneOrFiftyAndNull = "1".equals(refValue) && comparedValue == null
                    || refValue == null && "1".equals(comparedValue) ||
                    "50".equals(refValue) && comparedValue == null || refValue == null && "50".equals(comparedValue);

            if ((isNameField && isCaseInsensitive && refValue != null && refValue.equalsIgnoreCase(comparedValue)) ||
                    (isStartOrIncrementField && isOneOrFiftyAndNull)) {
                ignoredDifferenceFields.add(field);
            }
        }
        ignoredDifferenceFields.forEach(differences::removeDifference);
    }

}

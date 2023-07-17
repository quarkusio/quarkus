package io.quarkus.liquibase;

import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import io.quarkus.liquibase.runtime.LiquibaseConfig;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.ChangeSetStatus;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ResourceAccessor;

/**
 * The quarkus liquibase context
 */
public class LiquibaseContext extends Liquibase implements AutoCloseable {

    /**
     * The liquibase configuration
     */
    private LiquibaseConfig config;

    /**
     * The default constructor
     *
     * @param config the liquibase configuration
     * @param resourceAccessor the liquibase resource accessor
     * @param database the liquibase database
     */
    public LiquibaseContext(LiquibaseConfig config, ResourceAccessor resourceAccessor, Database database) {
        super(config.changeLog, resourceAccessor, database);
        this.config = config;
    }

    /**
     * Gets the liquibase configuration
     *
     * @return the liquibase configuration
     */
    public LiquibaseConfig getConfiguration() {
        return config;
    }

    /**
     * Gets the change log file path
     *
     * @return the change log file path
     */
    public String getChangeLog() {
        return config.changeLog;
    }

    /**
     * Creates the default labels base on the configuration
     *
     * @return the label expression
     */
    public LabelExpression createLabels() {
        return new LabelExpression(config.labels);
    }

    /**
     * Creates the default contexts base on the configuration
     *
     * @return the contexts
     */
    public Contexts createContexts() {
        return new Contexts(config.contexts);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#getChangeSetStatuses(Contexts, LabelExpression, boolean)} method
     *
     * @return the list of change set
     * @throws LiquibaseException if the method fails
     */
    public List<ChangeSetStatus> getChangeSetStatuses() throws LiquibaseException {
        return getChangeSetStatuses(createContexts(), createLabels(), true);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#update(Contexts, LabelExpression)} method
     *
     * @throws LiquibaseException if the method fails
     */
    public void update() throws LiquibaseException {
        update(createContexts(), createLabels());
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#listUnrunChangeSets(Contexts, LabelExpression, boolean)} method
     *
     * @return the list of change set
     * @throws LiquibaseException if the method fails
     */
    public List<ChangeSet> listUnrunChangeSets() throws LiquibaseException {
        return listUnrunChangeSets(createContexts(), createLabels(), true);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#update(String, Contexts, LabelExpression, Writer)} method
     *
     * @param tag the tag
     * @param output the output
     * @throws LiquibaseException if the method fails
     */
    public void update(String tag, Writer output) throws LiquibaseException {
        update(tag, createContexts(), createLabels(), output);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#rollback(int, String, Contexts, LabelExpression)} method.
     *
     * @param changesToApply changes to apply
     * @throws LiquibaseException if the method fails
     */
    public void update(int changesToApply) throws LiquibaseException {
        update(changesToApply, createContexts(), createLabels());
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#rollback(int, String, Contexts, LabelExpression, Writer)} method.
     *
     * @param changesToApply changes to apply
     * @param output the output
     * @throws LiquibaseException if the method fails
     */
    public void update(int changesToApply, Writer output) throws LiquibaseException {
        this.update(changesToApply, createContexts(), createLabels(), output);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#rollback(int, String, Contexts, LabelExpression, Writer)} method.
     *
     * @param changesToRollback changes to roll back
     * @param rollbackScript the rollback script
     * @param output the output
     * @throws LiquibaseException if the method fails
     */
    public void rollback(int changesToRollback, String rollbackScript, Writer output) throws LiquibaseException {
        rollback(changesToRollback, rollbackScript, createContexts(), createLabels(), output);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#rollback(int, String, Contexts, LabelExpression)} method.
     *
     * @param changesToRollback changes to roll back
     * @param rollbackScript the rollback script
     * @throws LiquibaseException if the method fails
     */
    public void rollback(int changesToRollback, String rollbackScript) throws LiquibaseException {
        rollback(changesToRollback, rollbackScript, createContexts(), createLabels());
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#rollback(String, String, Contexts, LabelExpression, Writer)} method.
     *
     * @param tagToRollBackTo tag to roll back to
     * @param rollbackScript the rollback script
     * @param output the output
     * @throws LiquibaseException if the method fails
     */
    public void rollback(String tagToRollBackTo, String rollbackScript, Writer output) throws LiquibaseException {
        rollback(tagToRollBackTo, rollbackScript, createContexts(), createLabels(), output);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#rollback(String, String, Contexts, LabelExpression)} method.
     *
     * @param tagToRollBackTo tag to roll back to
     * @param rollbackScript the rollback script
     * @throws LiquibaseException if the method fails
     */
    public void rollback(String tagToRollBackTo, String rollbackScript) throws LiquibaseException {
        rollback(tagToRollBackTo, rollbackScript, createContexts(), createLabels());
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#rollback(Date, String, Contexts, LabelExpression, Writer)} method.
     *
     * @param output the output
     * @param dateToRollBackTo date to roll back to
     * @param rollbackScript the rollback script
     * @throws LiquibaseException if the method fails
     */
    public void rollback(Date dateToRollBackTo, String rollbackScript, Writer output) throws LiquibaseException {
        rollback(dateToRollBackTo, rollbackScript, createContexts(), createLabels(), output);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#rollback(Date, String, Contexts, LabelExpression, Writer)} method.
     *
     * @param output the output
     * @param dateToRollBackTo date to roll back to
     * @param rollbackScript the rollback script
     * @param contexts the contexts
     * @param labelExpression the label expression
     * @throws LiquibaseException if the method fails
     */
    public void rollback(Date dateToRollBackTo, String rollbackScript, Contexts contexts, LabelExpression labelExpression,
            Writer output) throws LiquibaseException {
        rollback(dateToRollBackTo, rollbackScript, createContexts(), createLabels());
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#rollback(Date, String, Contexts, LabelExpression)} method.
     *
     * @param dateToRollBackTo date to roll back to
     * @param rollbackScript the rollback script
     * @throws LiquibaseException if the method fails
     */
    public void rollback(Date dateToRollBackTo, String rollbackScript) throws LiquibaseException {
        rollback(dateToRollBackTo, rollbackScript, createContexts(), createLabels());
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#changeLogSync(Contexts, LabelExpression)} method.
     *
     * @throws LiquibaseException if the method fails
     */
    public void changeLogSync() throws LiquibaseException {
        changeLogSync(createContexts(), createLabels());
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#changeLogSync(Contexts, LabelExpression, Writer)} method.
     *
     * @param output the output
     * @throws LiquibaseException if the method fails
     */
    public void changeLogSync(Writer output) throws LiquibaseException {
        changeLogSync(createContexts(), createLabels(), output);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#markNextChangeSetRan(Contexts, LabelExpression, Writer)} method.
     *
     * @param output the output
     * @throws LiquibaseException if the method fails
     */
    public void markNextChangeSetRan(Writer output) throws LiquibaseException {
        markNextChangeSetRan(createContexts(), createLabels(), output);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#markNextChangeSetRan(Contexts, LabelExpression)} method.
     *
     * @throws LiquibaseException if the method fails
     */
    public void markNextChangeSetRan() throws LiquibaseException {
        markNextChangeSetRan(createContexts(), createLabels());
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#futureRollbackSQL(String, Contexts, LabelExpression, Writer)} method.
     *
     * @param tag the tag
     * @param output the output
     * @throws LiquibaseException if the method fails
     */
    public void futureRollbackSQL(String tag, Writer output) throws LiquibaseException {
        futureRollbackSQL(tag, createContexts(), createLabels(), output);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#futureRollbackSQL(Integer, Contexts, LabelExpression, Writer, boolean)}
     * method.
     *
     * @param count the count
     * @param output the output
     * @param checkLiquibaseTables check liquibase tables
     * @throws LiquibaseException if the method fails
     */
    public void futureRollbackSQL(Integer count, Writer output, boolean checkLiquibaseTables) throws LiquibaseException {
        futureRollbackSQL(count, createContexts(), createLabels(), output, checkLiquibaseTables);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#updateTestingRollback(String, Contexts, LabelExpression)} method.
     *
     * @throws LiquibaseException if the method fails.
     */
    public void updateTestingRollback() throws LiquibaseException {
        updateTestingRollback(null, createContexts(), createLabels());
    }

    /**
     * Implementation of the
     * {@link liquibase.Liquibase#checkLiquibaseTables(boolean, DatabaseChangeLog, Contexts, LabelExpression)}
     * method.
     *
     * @param updateExistingNullChecksums update existing null checksums
     * @param databaseChangeLog database change log
     * @throws LiquibaseException if the method fails.
     */
    public void checkLiquibaseTables(boolean updateExistingNullChecksums, DatabaseChangeLog databaseChangeLog)
            throws LiquibaseException {
        checkLiquibaseTables(updateExistingNullChecksums, databaseChangeLog, createContexts(), createLabels());
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#reportStatus(boolean, Contexts, Writer)} method.
     *
     * @param verbose the verbose flag
     * @param output the output
     * @throws LiquibaseException if the method fails.
     */
    public void reportStatus(boolean verbose, Writer output) throws LiquibaseException {
        reportStatus(verbose, createContexts(), createLabels(), output);
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#listUnexpectedChangeSets(Contexts, LabelExpression)} method.
     *
     * @return the collection of the ran change sets
     * @throws LiquibaseException if the method fails.
     */
    public Collection<RanChangeSet> listUnexpectedChangeSets() throws LiquibaseException {
        return listUnexpectedChangeSets(createContexts(), createLabels());
    }

    /**
     * Implementation of the {@link liquibase.Liquibase#reportStatus(boolean, Contexts, LabelExpression, Writer)} method
     *
     * @param verbose the verbose flag
     * @param output the writer output
     * @throws LiquibaseException if the method fails.
     */
    public void reportUnexpectedChangeSets(boolean verbose, Writer output) throws LiquibaseException {
        reportUnexpectedChangeSets(verbose, createContexts(), createLabels(), output);
    }

    /**
     * Close the database connection for the liquibase instance.
     *
     * @throws LiquibaseException if the method fails
     */
    public void close() throws LiquibaseException {
        if (getDatabase() != null) {
            getDatabase().close();
        }
    }
}

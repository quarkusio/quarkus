package io.quarkus.liquibase;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Map;

import javax.sql.DataSource;

import io.quarkus.liquibase.runtime.LiquibaseConfig;
import io.quarkus.runtime.util.StringUtil;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;

public class LiquibaseFactory {

    private final DataSource dataSource;

    private final String dataSourceName;

    private final LiquibaseConfig config;

    public LiquibaseFactory(LiquibaseConfig config, DataSource datasource, String dataSourceName) {
        this.config = config;
        this.dataSource = datasource;
        this.dataSourceName = dataSourceName;
    }

    private ResourceAccessor resolveResourceAccessor() throws FileNotFoundException {

        if (config.changeLog.startsWith("classpath:")) {
            return new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader());
        }

        if (!config.changeLog.startsWith("filesystem:") &&
                config.searchPath.size() == 1 &&
                config.searchPath.get(0).equals("/")) {
            return new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader());
        }

        CompositeResourceAccessor compositeResourceAccessor = new CompositeResourceAccessor();
        compositeResourceAccessor
                .addResourceAccessor(new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()));

        for (String searchPath : config.searchPath) {
            compositeResourceAccessor.addResourceAccessor(new DirectoryResourceAccessor(Paths.get(searchPath)));
        }

        return compositeResourceAccessor;
    }

    private String parseChangeLog(String changeLog) {
        if (changeLog.startsWith("filesystem:")) {
            return StringUtil.changePrefix(changeLog, "filesystem:", "");
        }

        if (changeLog.startsWith("classpath:")) {
            return StringUtil.changePrefix(changeLog, "classpath:", "");
        }

        return changeLog;
    }

    public Liquibase createLiquibase() {
        try (ResourceAccessor resourceAccessor = resolveResourceAccessor()) {
            String parsedChangeLog = parseChangeLog(config.changeLog);

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()));

            if (database != null) {
                database.setDatabaseChangeLogLockTableName(config.databaseChangeLogLockTableName);
                database.setDatabaseChangeLogTableName(config.databaseChangeLogTableName);
                config.liquibaseCatalogName.ifPresent(database::setLiquibaseCatalogName);
                config.liquibaseSchemaName.ifPresent(database::setLiquibaseSchemaName);
                config.liquibaseTablespaceName.ifPresent(database::setLiquibaseTablespaceName);

                if (config.defaultCatalogName.isPresent()) {
                    database.setDefaultCatalogName(config.defaultCatalogName.get());
                }
                if (config.defaultSchemaName.isPresent()) {
                    database.setDefaultSchemaName(config.defaultSchemaName.get());
                }
            }
            Liquibase liquibase = new Liquibase(parsedChangeLog, resourceAccessor, database);

            for (Map.Entry<String, String> entry : config.changeLogParameters.entrySet()) {
                liquibase.getChangeLogParameters().set(entry.getKey(), entry.getValue());
            }

            return liquibase;

        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public LiquibaseConfig getConfiguration() {
        return config;
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

    public String getDataSourceName() {
        return dataSourceName;
    }
}

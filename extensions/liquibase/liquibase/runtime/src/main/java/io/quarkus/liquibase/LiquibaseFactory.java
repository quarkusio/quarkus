package io.quarkus.liquibase;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import io.agroal.api.AgroalDataSource;
import io.quarkus.liquibase.common.runtime.NativeImageResourceAccessor;
import io.quarkus.liquibase.runtime.LiquibaseConfig;
import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.ResettableSystemProperties;
import io.quarkus.runtime.util.StringUtil;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
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
        var rootAccessor = new CompositeResourceAccessor();
        return ImageMode.current().isNativeImage()
                ? nativeImageResourceAccessor(rootAccessor)
                : defaultResourceAccessor(rootAccessor);
    }

    private ResourceAccessor defaultResourceAccessor(CompositeResourceAccessor rootAccessor)
            throws FileNotFoundException {

        rootAccessor.addResourceAccessor(
                new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()));

        if (!config.changeLog.startsWith("filesystem:") && config.searchPath.isEmpty()) {
            return rootAccessor;
        }

        if (config.searchPath.isEmpty()) {
            return rootAccessor.addResourceAccessor(
                    new DirectoryResourceAccessor(
                            Paths.get(StringUtil
                                    .changePrefix(config.changeLog, "filesystem:", ""))
                                    .getParent()));
        }

        for (String searchPath : config.searchPath.get()) {
            rootAccessor.addResourceAccessor(new DirectoryResourceAccessor(Paths.get(searchPath)));
        }
        return rootAccessor;
    }

    private ResourceAccessor nativeImageResourceAccessor(CompositeResourceAccessor rootAccessor) {
        return rootAccessor.addResourceAccessor(new NativeImageResourceAccessor());
    }

    private String parseChangeLog(String changeLog) {
        if (changeLog.startsWith("filesystem:") && config.searchPath.isEmpty()) {
            return Paths.get(StringUtil.changePrefix(changeLog, "filesystem:", ""))
                    .getFileName().toString();
        }

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

            Database database;

            if (config.username.isPresent() && config.password.isPresent()) {
                AgroalDataSource agroalDataSource = dataSource.unwrap(AgroalDataSource.class);
                String jdbcUrl = agroalDataSource.getConfiguration().connectionPoolConfiguration()
                        .connectionFactoryConfiguration().jdbcUrl();
                Connection connection = DriverManager.getConnection(jdbcUrl, config.username.get(), config.password.get());

                database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(
                                new JdbcConnection(connection));

            } else {
                database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()));
            }

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
            Liquibase liquibase = new QuarkusLiquibase(parsedChangeLog, resourceAccessor, database,
                    createResettableSystemProperties());

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

    private ResettableSystemProperties createResettableSystemProperties() {
        Map<String, String> resettableProperties = new HashMap<>();

        if (config.allowDuplicatedChangesetIdentifiers.isPresent()) {
            resettableProperties.put("liquibase.allowDuplicatedChangesetIdentifiers",
                    config.allowDuplicatedChangesetIdentifiers.get().toString());
        }

        if (!config.secureParsing) {
            resettableProperties.put("liquibase.secureParsing", "false");
        }

        return new ResettableSystemProperties(resettableProperties);
    }

    private static class QuarkusLiquibase extends Liquibase {

        private final ResettableSystemProperties resettableSystemProperties;

        public QuarkusLiquibase(String changeLogFile, ResourceAccessor resourceAccessor, Database database,
                ResettableSystemProperties resettableSystemProperties) {
            super(changeLogFile, resourceAccessor, database);
            this.resettableSystemProperties = resettableSystemProperties;
        }

        @Override
        public void close() throws LiquibaseException {
            try {
                super.close();
            } finally {
                resettableSystemProperties.close();
            }
        }
    }
}

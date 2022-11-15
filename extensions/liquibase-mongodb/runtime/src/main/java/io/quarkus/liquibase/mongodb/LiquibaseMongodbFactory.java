package io.quarkus.liquibase.mongodb;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.util.AnnotationLiteral;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbBuildTimeConfig;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbConfig;
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;

public class LiquibaseMongodbFactory {

    private final String mongoClientName;
    private final Optional<String> defaultDatabaseName;
    private final LiquibaseMongodbConfig liquibaseMongodbConfig;
    private final LiquibaseMongodbBuildTimeConfig liquibaseMongodbBuildTimeConfig;

    public LiquibaseMongodbFactory(LiquibaseMongodbConfig config,
            LiquibaseMongodbBuildTimeConfig liquibaseMongodbBuildTimeConfig, String mongoClientName,
            Optional<String> defaultDatabaseName) {
        this.liquibaseMongodbConfig = config;
        this.liquibaseMongodbBuildTimeConfig = liquibaseMongodbBuildTimeConfig;
        this.mongoClientName = mongoClientName;
        this.defaultDatabaseName = defaultDatabaseName;
    }

    public Liquibase createLiquibase() {
        String databaseName = this.defaultDatabaseName.orElseThrow(() -> {
            String propertyName = MongoClientBeanUtil.isDefault(this.mongoClientName)
                    ? "quarkus.mongodb.database"
                    : "quarkus.mongodb." + this.mongoClientName + ".database";
            return new IllegalArgumentException("Config property '" + propertyName + "' must be defined");
        });

        return createLiquibase(databaseName);
    }

    public Liquibase createLiquibase(String databaseName) {
        return createLiquibase(databaseName, liquibaseMongodbBuildTimeConfig.changeLog);
    }

    public Liquibase createLiquibase(String databaseName, String changeLog) {
        return createLiquibase(databaseName, changeLog, liquibaseMongodbConfig.changeLogParameters);
    }

    public Liquibase createLiquibase(String databaseName, String changeLog, Map<String, String> changeLogParameters) {
        if (databaseName == null) {
            throw new IllegalArgumentException("'databaseName' cannot be null");
        }

        if (changeLog == null) {
            throw new IllegalArgumentException("'changelog' file cannot be null");
        }

        MongoClient mongoClient = Arc.container()
                .instance(MongoClient.class, getLiteral()).get();

        try (ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader())) {
            MongoConnection connection = new MongoConnection() {
                @Override
                public void close() throws DatabaseException {
                    // Ignore
                }
            };
            connection.setMongoClient(mongoClient);
            connection.setMongoDatabase(mongoClient.getDatabase(databaseName));

            Database database = new MongoLiquibaseDatabase() {
                @Override
                public void close() throws DatabaseException {
                    // Ignore
                }
            };
            database.setConnection(connection);

            liquibaseMongodbConfig.liquibaseCatalogName.ifPresent(database::setLiquibaseCatalogName);
            liquibaseMongodbConfig.liquibaseSchemaName.ifPresent(database::setLiquibaseSchemaName);
            liquibaseMongodbConfig.liquibaseTablespaceName.ifPresent(database::setLiquibaseTablespaceName);

            if (liquibaseMongodbConfig.defaultCatalogName.isPresent()) {
                database.setDefaultCatalogName(liquibaseMongodbConfig.defaultCatalogName.get());
            }
            if (liquibaseMongodbConfig.defaultSchemaName.isPresent()) {
                database.setDefaultSchemaName(liquibaseMongodbConfig.defaultSchemaName.get());
            }
            Liquibase liquibase = new Liquibase(changeLog, resourceAccessor, database);

            if (changeLogParameters != null) {
                for (Map.Entry<String, String> entry : changeLogParameters.entrySet()) {
                    liquibase.getChangeLogParameters().set(entry.getKey(), entry.getValue());
                }
            }

            return liquibase;

        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private AnnotationLiteral getLiteral() {
        return MongoClientBeanUtil.isDefault(mongoClientName) ? Default.Literal.INSTANCE : NamedLiteral.of(mongoClientName);
    }

    public LiquibaseMongodbConfig getConfiguration() {
        return liquibaseMongodbConfig;
    }

    /**
     * Creates the default labels base on the configuration
     *
     * @return the label expression
     */
    public LabelExpression createLabels() {
        return new LabelExpression(liquibaseMongodbConfig.labels.orElse(null));
    }

    /**
     * Creates the default contexts base on the configuration
     *
     * @return the contexts
     */
    public Contexts createContexts() {
        return new Contexts(liquibaseMongodbConfig.contexts.orElse(null));
    }
}

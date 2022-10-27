package io.quarkus.liquibase.mongodb;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbBuildTimeConfig;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbConfig;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;

public class LiquibaseMongodbFactory {

    private final MongoClientConfig mongoClientConfig;
    private final LiquibaseMongodbConfig liquibaseMongodbConfig;
    private final LiquibaseMongodbBuildTimeConfig liquibaseMongodbBuildTimeConfig;

    //connection-string format, see https://docs.mongodb.com/manual/reference/connection-string/
    Pattern HAS_DB = Pattern
            .compile("(?<prefix>mongodb://|mongodb\\+srv://)(?<hosts>[^/]*)(?<slash>[/]?)(?<db>[^?]*)(?<options>\\??.*)");

    public LiquibaseMongodbFactory(LiquibaseMongodbConfig config,
            LiquibaseMongodbBuildTimeConfig liquibaseMongodbBuildTimeConfig, MongoClientConfig mongoClientConfig) {
        this.liquibaseMongodbConfig = config;
        this.liquibaseMongodbBuildTimeConfig = liquibaseMongodbBuildTimeConfig;
        this.mongoClientConfig = mongoClientConfig;
    }

    public Liquibase createLiquibase() {
        try (ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader())) {
            String connectionString = this.mongoClientConfig.connectionString.orElse("mongodb://localhost:27017");

            Matcher matcher = HAS_DB.matcher(connectionString);
            if (!matcher.matches() || matcher.group("db") == null || matcher.group("db").isEmpty()) {
                connectionString = matcher.replaceFirst(
                        "${prefix}${hosts}/"
                                + this.mongoClientConfig.database
                                        .orElseThrow(() -> new IllegalArgumentException("Config property " +
                                                "'quarkus.mongodb.database' must be defined when no database exist in the connection string"))
                                + "${options}");
            }
            if (mongoClientConfig.credentials.authSource.isPresent()) {
                boolean alreadyHasQueryParams = connectionString.contains("?");
                connectionString += (alreadyHasQueryParams ? "&" : "?") + "authSource="
                        + mongoClientConfig.credentials.authSource.get();
            }

            Database database = DatabaseFactory.getInstance().openDatabase(connectionString,
                    this.mongoClientConfig.credentials.username.orElse(null),
                    this.mongoClientConfig.credentials.password.orElse(null),
                    null, resourceAccessor);

            if (database != null) {
                liquibaseMongodbConfig.liquibaseCatalogName.ifPresent(database::setLiquibaseCatalogName);
                liquibaseMongodbConfig.liquibaseSchemaName.ifPresent(database::setLiquibaseSchemaName);
                liquibaseMongodbConfig.liquibaseTablespaceName.ifPresent(database::setLiquibaseTablespaceName);

                if (liquibaseMongodbConfig.defaultCatalogName.isPresent()) {
                    database.setDefaultCatalogName(liquibaseMongodbConfig.defaultCatalogName.get());
                }
                if (liquibaseMongodbConfig.defaultSchemaName.isPresent()) {
                    database.setDefaultSchemaName(liquibaseMongodbConfig.defaultSchemaName.get());
                }
            }
            Liquibase liquibase = new Liquibase(liquibaseMongodbBuildTimeConfig.changeLog, resourceAccessor, database);

            for (Map.Entry<String, String> entry : liquibaseMongodbConfig.changeLogParameters.entrySet()) {
                liquibase.getChangeLogParameters().set(entry.getKey(), entry.getValue());
            }

            return liquibase;

        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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

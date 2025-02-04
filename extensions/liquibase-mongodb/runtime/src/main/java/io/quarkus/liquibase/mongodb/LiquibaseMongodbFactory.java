package io.quarkus.liquibase.mongodb;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbBuildTimeConfig;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbConfig;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.runtime.util.StringUtil;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;

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

    private ResourceAccessor resolveResourceAccessor() throws FileNotFoundException {

        CompositeResourceAccessor compositeResourceAccessor = new CompositeResourceAccessor();
        compositeResourceAccessor
                .addResourceAccessor(new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()));

        if (!liquibaseMongodbBuildTimeConfig.changeLog().startsWith("filesystem:")
                && liquibaseMongodbBuildTimeConfig.searchPath().isEmpty()) {
            return compositeResourceAccessor;
        }

        if (liquibaseMongodbBuildTimeConfig.searchPath().isEmpty()) {
            compositeResourceAccessor.addResourceAccessor(
                    new DirectoryResourceAccessor(
                            Paths.get(StringUtil.changePrefix(liquibaseMongodbBuildTimeConfig.changeLog(), "filesystem:", ""))
                                    .getParent()));
            return compositeResourceAccessor;
        }

        for (String searchPath : liquibaseMongodbBuildTimeConfig.searchPath().get()) {
            compositeResourceAccessor.addResourceAccessor(new DirectoryResourceAccessor(Paths.get(searchPath)));
        }

        return compositeResourceAccessor;
    }

    private String parseChangeLog(String changeLog) {

        if (changeLog.startsWith("filesystem:") && liquibaseMongodbBuildTimeConfig.searchPath().isEmpty()) {
            return Paths.get(StringUtil.changePrefix(changeLog, "filesystem:", "")).getFileName().toString();
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
            String parsedChangeLog = parseChangeLog(liquibaseMongodbBuildTimeConfig.changeLog());
            String connectionString = this.mongoClientConfig.connectionString().orElse("mongodb://localhost:27017");

            // Every MongoDB client configuration must be added to the connection string, we didn't add all as it would be too much to support.
            // For reference, all connections string options can be found here: https://www.mongodb.com/docs/manual/reference/connection-string/#connection-string-options.

            Matcher matcher = HAS_DB.matcher(connectionString);
            if (!matcher.matches() || matcher.group("db") == null || matcher.group("db").isEmpty()) {
                connectionString = matcher.replaceFirst(
                        "${prefix}${hosts}/"
                                + this.mongoClientConfig.database()
                                        .orElseThrow(() -> new IllegalArgumentException("Config property " +
                                                "'quarkus.mongodb.database' must be defined when no database exist in the connection string"))
                                + "${options}");
            }
            if (mongoClientConfig.credentials().authSource().isPresent()) {
                boolean alreadyHasQueryParams = connectionString.contains("?");
                connectionString += (alreadyHasQueryParams ? "&" : "?") + "authSource="
                        + mongoClientConfig.credentials().authSource().get();
            }
            if (mongoClientConfig.credentials().authMechanism().isPresent()) {
                boolean alreadyHasQueryParams = connectionString.contains("?");
                connectionString += (alreadyHasQueryParams ? "&" : "?") + "authMechanism="
                        + mongoClientConfig.credentials().authMechanism().get();
            }
            if (!mongoClientConfig.credentials().authMechanismProperties().isEmpty()) {
                boolean alreadyHasQueryParams = connectionString.contains("?");
                connectionString += (alreadyHasQueryParams ? "&" : "?") + "authMechanismProperties="
                        + mongoClientConfig.credentials().authMechanismProperties().entrySet().stream()
                                .map(prop -> prop.getKey() + ":" + prop.getValue()).collect(Collectors.joining(","));
            }

            Database database = DatabaseFactory.getInstance().openDatabase(connectionString,
                    this.mongoClientConfig.credentials().username().orElse(null),
                    this.mongoClientConfig.credentials().password().orElse(null),
                    null, resourceAccessor);

            if (database != null) {
                liquibaseMongodbConfig.liquibaseCatalogName().ifPresent(database::setLiquibaseCatalogName);
                liquibaseMongodbConfig.liquibaseSchemaName().ifPresent(database::setLiquibaseSchemaName);
                liquibaseMongodbConfig.liquibaseTablespaceName().ifPresent(database::setLiquibaseTablespaceName);

                if (liquibaseMongodbConfig.defaultCatalogName().isPresent()) {
                    database.setDefaultCatalogName(liquibaseMongodbConfig.defaultCatalogName().get());
                }
                if (liquibaseMongodbConfig.defaultSchemaName().isPresent()) {
                    database.setDefaultSchemaName(liquibaseMongodbConfig.defaultSchemaName().get());
                }
            }
            Liquibase liquibase = new Liquibase(parsedChangeLog, resourceAccessor, database);

            for (Map.Entry<String, String> entry : liquibaseMongodbConfig.changeLogParameters().entrySet()) {
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
        return new LabelExpression(liquibaseMongodbConfig.labels().orElse(null));
    }

    /**
     * Creates the default contexts base on the configuration
     *
     * @return the contexts
     */
    public Contexts createContexts() {
        return new Contexts(liquibaseMongodbConfig.contexts().orElse(null));
    }
}

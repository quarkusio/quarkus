package io.quarkus.liquibase.mongodb;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.liquibase.common.runtime.NativeImageResourceAccessor;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbBuildTimeConfig;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbConfig;
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongoClients;
import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.util.StringUtil;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;

public class LiquibaseMongodbFactory {

    //connection-string format, see https://docs.mongodb.com/manual/reference/connection-string/
    private static final Pattern HAS_DB = Pattern
            .compile("(?<prefix>mongodb://|mongodb\\+srv://)(?<hosts>[^/]*)(?<slash>[/]?)(?<db>[^?]*)(?<options>\\??.*)");
    private final LiquibaseMongodbConfig liquibaseMongodbConfig;
    private final LiquibaseMongodbBuildTimeConfig liquibaseMongodbBuildTimeConfig;
    private final MongodbConfig mongodbConfig;

    public LiquibaseMongodbFactory(LiquibaseMongodbConfig config,
            LiquibaseMongodbBuildTimeConfig liquibaseMongodbBuildTimeConfig, MongodbConfig mongodbConfig) {
        this.liquibaseMongodbConfig = config;
        this.liquibaseMongodbBuildTimeConfig = liquibaseMongodbBuildTimeConfig;
        this.mongodbConfig = mongodbConfig;
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

        if (!liquibaseMongodbBuildTimeConfig.changeLog().startsWith("filesystem:")
                && liquibaseMongodbBuildTimeConfig.searchPath().isEmpty()) {
            return rootAccessor;
        }

        if (liquibaseMongodbBuildTimeConfig.searchPath().isEmpty()) {
            return rootAccessor.addResourceAccessor(
                    new DirectoryResourceAccessor(
                            Paths.get(StringUtil
                                    .changePrefix(liquibaseMongodbBuildTimeConfig.changeLog(), "filesystem:", ""))
                                    .getParent()));
        }

        for (String searchPath : liquibaseMongodbBuildTimeConfig.searchPath().get()) {
            rootAccessor.addResourceAccessor(new DirectoryResourceAccessor(Paths.get(searchPath)));
        }
        return rootAccessor;
    }

    private ResourceAccessor nativeImageResourceAccessor(CompositeResourceAccessor rootAccessor) {
        return rootAccessor.addResourceAccessor(new NativeImageResourceAccessor());
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
            MongoClients mongoClients = Arc.container().instance(MongoClients.class).get();
            String mongoClientName;
            MongoClientConfig mongoClientConfig;
            if (liquibaseMongodbConfig.mongoClientName().isPresent()) {
                mongoClientName = liquibaseMongodbConfig.mongoClientName().get();
                mongoClientConfig = mongodbConfig.mongoClientConfigs().get(mongoClientName);
                if (mongoClientConfig == null) {
                    throw new IllegalArgumentException("Mongo client named '%s' not found".formatted(mongoClientName));
                }
            } else {
                mongoClientConfig = mongodbConfig.defaultMongoClientConfig();
                mongoClientName = MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME;
            }
            String parsedChangeLog = parseChangeLog(liquibaseMongodbBuildTimeConfig.changeLog());
            String connectionString = mongoClientConfig.connectionString().orElse("mongodb://localhost:27017");
            Matcher matcher = HAS_DB.matcher(connectionString);
            Optional<String> maybeDatabase = mongoClientConfig.database();
            if (maybeDatabase.isEmpty()) {
                if (matcher.matches() && !StringUtil.isNullOrEmpty(matcher.group("db"))) {
                    maybeDatabase = Optional.of(matcher.group("db"));
                } else {
                    throw new IllegalArgumentException("Config property 'quarkus.mongodb.database' must " +
                            "be defined when no database exist in the connection string");
                }
            }
            Database database = createDatabase(mongoClients, mongoClientName, maybeDatabase.get());
            if (liquibaseMongodbConfig.liquibaseCatalogName().isPresent()) {
                database.setLiquibaseCatalogName(liquibaseMongodbConfig.liquibaseCatalogName().get());
            }
            if (liquibaseMongodbConfig.liquibaseSchemaName().isPresent()) {
                database.setLiquibaseSchemaName(liquibaseMongodbConfig.liquibaseSchemaName().get());
            }
            if (liquibaseMongodbConfig.liquibaseTablespaceName().isPresent()) {
                database.setLiquibaseTablespaceName(liquibaseMongodbConfig.liquibaseTablespaceName().get());
            }
            if (liquibaseMongodbConfig.defaultCatalogName().isPresent()) {
                database.setDefaultCatalogName(liquibaseMongodbConfig.defaultCatalogName().get());
            }
            if (liquibaseMongodbConfig.defaultSchemaName().isPresent()) {
                database.setDefaultSchemaName(liquibaseMongodbConfig.defaultSchemaName().get());
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

    private Database createDatabase(MongoClients clients, String clientName, String databaseName) {
        MongoConnection databaseConnection = new MongoConnection();
        MongoClient mongoClient = clients.createMongoClient(clientName);
        databaseConnection.setMongoClient(mongoClient);
        databaseConnection.setMongoDatabase(mongoClient.getDatabase(databaseName));
        Database database = new MongoLiquibaseDatabase();
        database.setConnection(databaseConnection);
        return database;
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

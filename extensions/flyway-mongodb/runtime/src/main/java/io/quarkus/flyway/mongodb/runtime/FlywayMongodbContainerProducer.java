package io.quarkus.flyway.mongodb.runtime;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;

import com.mongodb.ConnectionString;

import io.quarkus.arc.All;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.flyway.mongodb.FlywayMongodbClient;
import io.quarkus.flyway.mongodb.FlywayMongodbConfigurationCustomizer;
import io.quarkus.mongodb.runtime.CredentialConfig;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongoConfig;

/**
 * This class is sort of a producer for {@link org.flywaydb.core.Flyway}.
 *
 * It isn't a CDI producer in the literal sense, but it is marked as a bean
 * and its {@code createFlywayContainer} method is called at runtime in order to produce
 * the actual {@code Flyway} objects.
 *
 * CDI scopes and qualifiers are set up at build-time, which is why this class is devoid of
 * any CDI annotations.
 *
 */
public class FlywayMongodbContainerProducer {

    private static final Logger LOG = Logger.getLogger(FlywayMongodbContainerProducer.class);

    private final FlywayMongodbBuildTimeConfig buildTimeConfig;
    private final FlywayMongodbRuntimeConfig runtimeConfig;
    private final MongoConfig mongoConfig;
    private final List<InstanceHandle<FlywayMongodbConfigurationCustomizer>> customizerInstances;

    public FlywayMongodbContainerProducer(FlywayMongodbBuildTimeConfig buildTimeConfig,
            FlywayMongodbRuntimeConfig runtimeConfig,
            MongoConfig mongoConfig,
            @All List<InstanceHandle<FlywayMongodbConfigurationCustomizer>> customizerInstances) {
        this.buildTimeConfig = buildTimeConfig;
        this.runtimeConfig = runtimeConfig;
        this.mongoConfig = mongoConfig;
        this.customizerInstances = customizerInstances;
    }

    public FlywayMongodbContainer createFlywayContainer(String clientName, boolean hasMigrations,
            Set<String> resourcesLocations) {
        FlywayMongodbClientBuildTimeConfig clientBuild = buildTimeConfig.clients().get(clientName);
        FlywayMongodbClientRuntimeConfig clientRuntime = runtimeConfig.clients().get(clientName);
        if (clientBuild == null || clientRuntime == null) {
            throw new IllegalStateException(
                    "No Flyway-MongoDB configuration for client '" + clientName + "'");
        }

        String connectionString = clientRuntime.connectionString()
                .orElseGet(() -> resolveConnectionString(clientName));
        ConnectionString parsed = new ConnectionString(connectionString);

        // The plugin runs .js migrations via the mongosh subprocess, which receives only the
        // URL — it does not see Flyway's defaultSchema. So the target database must be in the
        // URL path. Splice it in when missing and an explicit database is configured.
        MongoClientConfig mongoClientConfig = mongoConfig.clients().get(clientName);
        if (parsed.getDatabase() == null) {
            String database = clientRuntime.database()
                    .or(() -> (mongoClientConfig != null) ? mongoClientConfig.database() : Optional.empty())
                    .orElse(null);
            if (database != null && !database.isBlank()) {
                connectionString = appendDatabase(connectionString, database);
            }
        }

        // The flyway-database-nc-mongodb plugin only honors user/password when the URL
        // has no embedded credentials; otherwise it silently ignores them.
        String user = null;
        String password = null;
        if (mongoClientConfig != null && parsed.getCredential() == null) {
            CredentialConfig credentials = mongoClientConfig.credentials();
            user = credentials.username().orElse(null);
            password = credentials.password().orElse(null);
            if (credentials.authSource().isPresent()) {
                if (hasQueryParam(connectionString, "authSource")) {
                    LOG.debugf("Not forwarding quarkus.mongodb.%s.credentials.auth-source to Flyway because "
                            + "the connection string already specifies an authSource.", clientName);
                } else {
                    connectionString = appendQueryParam(connectionString, "authSource", credentials.authSource().get());
                }
            }
            warnUnsupportedCredentialSettings(clientName, credentials);
        }

        FlywayMongodbCreator creator = new FlywayMongodbCreator(clientRuntime, clientBuild,
                matchingCustomizers(clientName));
        Flyway flyway = creator.createFlyway(clientName, connectionString, user, password);

        return new FlywayMongodbContainer(flyway,
                clientRuntime.baselineAtStart(),
                clientRuntime.cleanAtStart().orElse(false),
                clientRuntime.cleanOnValidationError(),
                clientRuntime.migrateAtStart(),
                clientRuntime.repairAtStart(),
                clientRuntime.validateAtStart(),
                clientName,
                hasMigrations,
                resourcesLocations,
                clientRuntime.cleanDisabled());
    }

    static String appendDatabase(String connectionString, String database) {
        String encoded;
        try {
            encoded = new URI(null, null, database, null).getRawPath();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "MongoDB database name '" + database + "' cannot be percent-encoded for use in a connection string URL. "
                            + "Set quarkus.flyway-mongodb.connection-string to provide the full URL directly.",
                    e);
        }
        int query = connectionString.indexOf('?', connectionString.indexOf("://") + 3);
        if (query < 0) {
            return connectionString.endsWith("/")
                    ? connectionString + encoded
                    : connectionString + "/" + encoded;
        }
        String head = connectionString.substring(0, query);
        String tail = connectionString.substring(query);
        return (head.endsWith("/") ? head + encoded : head + "/" + encoded) + tail;
    }

    static String appendQueryParam(String connectionString, String name, String value) {
        // URLEncoder uses application/x-www-form-urlencoded (space -> '+'). MongoDB connection
        // strings follow RFC 3986 where space -> '%20'. Patch the one form-encoding-only
        // substitution so the result is a valid URI option value.
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
        int query = connectionString.indexOf('?', connectionString.indexOf("://") + 3);
        if (query < 0) {
            // no '?' yet — ensure the path segment exists, then start the query string
            String separator = hasPathSegment(connectionString) ? "?" : "/?";
            return connectionString + separator + name + "=" + encoded;
        }
        return connectionString + "&" + name + "=" + encoded;
    }

    static boolean hasPathSegment(String connectionString) {
        int schemeEnd = connectionString.indexOf("://");
        if (schemeEnd < 0) {
            return false;
        }
        return connectionString.indexOf('/', schemeEnd + 3) >= 0;
    }

    /**
     * Returns {@code true} if the connection string's query component contains the named option.
     * MongoDB URI option names are case-insensitive (per the MongoDB connection string spec).
     */
    static boolean hasQueryParam(String connectionString, String name) {
        int query = connectionString.indexOf('?', connectionString.indexOf("://") + 3);
        if (query < 0) {
            return false;
        }
        String lowerPrefix = name.toLowerCase(Locale.ROOT) + "=";
        for (String param : connectionString.substring(query + 1).split("&")) {
            if (param.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                return true;
            }
        }
        return false;
    }

    private static void warnUnsupportedCredentialSettings(String clientName, CredentialConfig credentials) {
        if (credentials.authMechanism().isPresent()) {
            LOG.warnf("Ignoring quarkus.mongodb.%s.credentials.auth-mechanism for Flyway: "
                    + "the flyway-database-nc-mongodb plugin always uses the default SCRAM mechanism.",
                    clientName);
        }
        if (!credentials.authMechanismProperties().isEmpty()) {
            LOG.warnf("Ignoring quarkus.mongodb.%s.credentials.auth-mechanism-properties for Flyway: "
                    + "the flyway-database-nc-mongodb plugin does not support custom mechanism properties.",
                    clientName);
        }
        if (credentials.credentialsProvider().isPresent() || credentials.credentialsProviderName().isPresent()) {
            LOG.warnf("Ignoring quarkus.mongodb.%s.credentials.credentials-provider* for Flyway: "
                    + "dynamic credentials providers (e.g. Vault) are not yet plumbed into Flyway-MongoDB. "
                    + "Embed credentials in the connection string or set quarkus.mongodb.%s.credentials.username/password directly.",
                    clientName, clientName);
        }
    }

    private String resolveConnectionString(String clientName) {
        MongoClientConfig clientConfig = mongoConfig.clients().get(clientName);
        if (clientConfig == null) {
            throw new IllegalStateException(
                    "No MongoDB client configuration for '" + clientName
                            + "'; either configure quarkus.mongodb."
                            + (MongoConfig.isDefaultClient(clientName) ? "" : clientName + ".")
                            + "connection-string"
                            + " or set quarkus.flyway-mongodb."
                            + (MongoConfig.isDefaultClient(clientName) ? "" : clientName + ".")
                            + "connection-string");
        }
        return clientConfig.connectionString().orElseThrow(() -> new IllegalStateException(
                "No connection string available for MongoDB client '" + clientName + "'"));
    }

    private List<FlywayMongodbConfigurationCustomizer> matchingCustomizers(String clientName) {
        if (customizerInstances == null || customizerInstances.isEmpty()) {
            return List.of();
        }
        List<FlywayMongodbConfigurationCustomizer> result = new ArrayList<>();
        for (InstanceHandle<FlywayMongodbConfigurationCustomizer> handle : customizerInstances) {
            var qualifiers = handle.getBean().getQualifiers();
            boolean hasClientQualifier = qualifiers.stream()
                    .anyMatch(q -> q instanceof FlywayMongodbClient fmc && fmc.value().equals(clientName));
            boolean hasDefaultQualifier = qualifiers.stream()
                    .noneMatch(q -> q instanceof FlywayMongodbClient);
            if (hasClientQualifier || (hasDefaultQualifier && MongoConfig.isDefaultClient(clientName))) {
                result.add(handle.get());
            }
        }
        return result;
    }
}

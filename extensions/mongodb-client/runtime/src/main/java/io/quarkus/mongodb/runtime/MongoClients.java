package io.quarkus.mongodb.runtime;

import static com.mongodb.AuthenticationMechanism.GSSAPI;
import static com.mongodb.AuthenticationMechanism.MONGODB_AWS;
import static com.mongodb.AuthenticationMechanism.MONGODB_X509;
import static com.mongodb.AuthenticationMechanism.PLAIN;
import static com.mongodb.AuthenticationMechanism.SCRAM_SHA_1;
import static com.mongodb.AuthenticationMechanism.SCRAM_SHA_256;
import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.jboss.logging.Logger;

import com.mongodb.AuthenticationMechanism;
import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ReadConcern;
import com.mongodb.ReadConcernLevel;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.ServerSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;
import com.mongodb.event.CommandListener;
import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.mongodb.health.MongoHealthCheck;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;

/**
 * This class is sort of a producer for {@link MongoClient} and {@link ReactiveMongoClient}.
 *
 * It isn't a CDI producer in the literal sense, but it is marked as a bean
 * and its {@code createMongoClient} and {@code createReactiveMongoClient} methods are called at runtime in order to produce
 * the actual client objects.
 *
 *
 */
@Singleton
public class MongoClients {

    private static final Logger LOGGER = Logger.getLogger(MongoClients.class.getName());
    private static final Pattern COLON_PATTERN = Pattern.compile(":");

    private final MongodbConfig mongodbConfig;
    private final MongoClientSupport mongoClientSupport;
    private final Instance<CodecProvider> codecProviders;
    private final Instance<PropertyCodecProvider> propertyCodecProviders;
    private final Instance<CommandListener> commandListeners;

    private final Map<String, MongoClient> mongoclients = new HashMap<>();
    private final Map<String, ReactiveMongoClient> reactiveMongoClients = new HashMap<>();

    public MongoClients(MongodbConfig mongodbConfig, MongoClientSupport mongoClientSupport,
            Instance<CodecProvider> codecProviders,
            Instance<PropertyCodecProvider> propertyCodecProviders,
            Instance<CommandListener> commandListeners) {
        this.mongodbConfig = mongodbConfig;
        this.mongoClientSupport = mongoClientSupport;
        this.codecProviders = codecProviders;
        this.propertyCodecProviders = propertyCodecProviders;
        this.commandListeners = commandListeners;

        try {
            //JDK bug workaround
            //https://github.com/quarkusio/quarkus/issues/14424
            //force class init to prevent possible deadlock when done by mongo threads
            Class.forName("sun.net.ext.ExtendedSocketOptions", true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("org.eclipse.microprofile.health.HealthCheck");
            InstanceHandle<MongoHealthCheck> instance = Arc.container()
                    .instance(MongoHealthCheck.class, Any.Literal.INSTANCE);
            if (instance.isAvailable()) {
                instance.get().configure(mongodbConfig);
            }
        } catch (ClassNotFoundException e) {
            // Ignored - no health check
        }
    }

    public MongoClient createMongoClient(String clientName) throws MongoException {
        MongoClientSettings mongoConfiguration = createMongoConfiguration(getMatchingMongoClientConfig(clientName));
        MongoClient client = com.mongodb.client.MongoClients.create(mongoConfiguration);
        mongoclients.put(clientName, client);
        return client;
    }

    public ReactiveMongoClient createReactiveMongoClient(String clientName)
            throws MongoException {
        MongoClientSettings mongoConfiguration = createMongoConfiguration(getMatchingMongoClientConfig(clientName));
        com.mongodb.reactivestreams.client.MongoClient client = com.mongodb.reactivestreams.client.MongoClients
                .create(mongoConfiguration);
        ReactiveMongoClientImpl reactive = new ReactiveMongoClientImpl(client);
        reactiveMongoClients.put(clientName, reactive);
        return reactive;
    }

    public MongoClientConfig getMatchingMongoClientConfig(String clientName) {
        return MongoClientBeanUtil.isDefault(clientName) ? mongodbConfig.defaultMongoClientConfig
                : mongodbConfig.mongoClientConfigs.get(clientName);
    }

    private static class ClusterSettingBuilder implements Block<ClusterSettings.Builder> {
        public ClusterSettingBuilder(MongoClientConfig config) {
            this.config = config;
        }

        private MongoClientConfig config;

        @Override
        public void apply(ClusterSettings.Builder builder) {
            Optional<String> maybeConnectionString = config.connectionString;
            if (!maybeConnectionString.isPresent()) {
                // Parse hosts
                List<ServerAddress> hosts = parseHosts(config.hosts);
                builder.hosts(hosts);

                if (hosts.size() == 1 && !config.replicaSetName.isPresent()) {
                    builder.mode(ClusterConnectionMode.SINGLE);
                } else {
                    builder.mode(ClusterConnectionMode.MULTIPLE);
                }
            }
            if (config.localThreshold.isPresent()) {
                builder.localThreshold(config.localThreshold.get().toMillis(), TimeUnit.MILLISECONDS);
            }

            config.replicaSetName.ifPresent(builder::requiredReplicaSetName);

            if (config.serverSelectionTimeout.isPresent()) {
                builder.serverSelectionTimeout(config.serverSelectionTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    private static class ConnectionPoolSettingsBuilder implements Block<ConnectionPoolSettings.Builder> {
        public ConnectionPoolSettingsBuilder(MongoClientConfig config, List<ConnectionPoolListener> connectionPoolListeners) {
            this.config = config;
            this.connectionPoolListeners = connectionPoolListeners;
        }

        private MongoClientConfig config;
        private List<ConnectionPoolListener> connectionPoolListeners;

        @Override
        public void apply(ConnectionPoolSettings.Builder builder) {
            config.maxPoolSize.ifPresent(builder::maxSize);
            config.minPoolSize.ifPresent(builder::minSize);
            if (config.maxConnectionIdleTime.isPresent()) {
                builder.maxConnectionIdleTime(config.maxConnectionIdleTime.get().toMillis(), TimeUnit.MILLISECONDS);
            }
            if (config.maxConnectionLifeTime.isPresent()) {
                builder.maxConnectionLifeTime(config.maxConnectionLifeTime.get().toMillis(), TimeUnit.MILLISECONDS);
            }
            if (config.maintenanceFrequency.isPresent()) {
                builder.maintenanceFrequency(config.maintenanceFrequency.get().toMillis(), TimeUnit.MILLISECONDS);
            }
            if (config.maintenanceInitialDelay.isPresent()) {
                builder.maintenanceInitialDelay(config.maintenanceInitialDelay.get().toMillis(), TimeUnit.MILLISECONDS);
            }
            for (ConnectionPoolListener connectionPoolListener : connectionPoolListeners) {
                builder.addConnectionPoolListener(connectionPoolListener);
            }
        }
    }

    private static class SslSettingsBuilder implements Block<SslSettings.Builder> {
        public SslSettingsBuilder(MongoClientConfig config, boolean disableSslSupport) {
            this.config = config;
            this.disableSslSupport = disableSslSupport;
        }

        private MongoClientConfig config;
        private boolean disableSslSupport;

        @Override
        public void apply(SslSettings.Builder builder) {
            builder.enabled(!disableSslSupport).invalidHostNameAllowed(config.tlsInsecure);
        }
    }

    private static class SocketSettingsBuilder implements Block<SocketSettings.Builder> {
        public SocketSettingsBuilder(MongoClientConfig config) {
            this.config = config;
        }

        private MongoClientConfig config;

        @Override
        public void apply(SocketSettings.Builder builder) {
            if (config.connectTimeout.isPresent()) {
                builder.connectTimeout((int) config.connectTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            }
            if (config.readTimeout.isPresent()) {
                builder.readTimeout((int) config.readTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    private static class ServerSettingsBuilder implements Block<ServerSettings.Builder> {
        public ServerSettingsBuilder(MongoClientConfig config) {
            this.config = config;
        }

        private MongoClientConfig config;

        @Override
        public void apply(ServerSettings.Builder builder) {
            if (config.heartbeatFrequency.isPresent()) {
                builder.heartbeatFrequency((int) config.heartbeatFrequency.get().toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    private MongoClientSettings createMongoConfiguration(MongoClientConfig config) {
        if (config == null) {
            throw new RuntimeException("mongo config is missing for creating mongo client.");
        }
        CodecRegistry defaultCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();

        MongoClientSettings.Builder settings = MongoClientSettings.builder();

        ConnectionString connectionString;
        Optional<String> maybeConnectionString = config.connectionString;
        if (maybeConnectionString.isPresent()) {
            connectionString = new ConnectionString(maybeConnectionString.get());
            settings.applyConnectionString(connectionString);
        }

        configureCodecRegistry(defaultCodecRegistry, settings);

        List<CommandListener> commandListenerList = new ArrayList<>();
        for (CommandListener commandListener : commandListeners) {
            commandListenerList.add(commandListener);
        }
        settings.commandListenerList(commandListenerList);

        config.applicationName.ifPresent(settings::applicationName);

        if (config.credentials != null) {
            MongoCredential credential = createMongoCredential(config);
            if (credential != null) {
                settings.credential(credential);
            }
        }

        if (config.writeConcern != null) {
            WriteConcernConfig wc = config.writeConcern;
            WriteConcern concern = (wc.safe ? WriteConcern.ACKNOWLEDGED : WriteConcern.UNACKNOWLEDGED)
                    .withJournal(wc.journal);

            if (wc.wTimeout.isPresent()) {
                concern = concern.withWTimeout(wc.wTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            }

            Optional<String> maybeW = wc.w;
            if (maybeW.isPresent()) {
                String w = maybeW.get();
                if ("majority".equalsIgnoreCase(w)) {
                    concern = concern.withW(w);
                } else {
                    int wInt = Integer.parseInt(w);
                    concern = concern.withW(wInt);
                }
            }
            settings.writeConcern(concern);
            settings.retryWrites(wc.retryWrites);
        }
        if (config.tls) {
            settings.applyToSslSettings(new SslSettingsBuilder(config, mongoClientSupport.isDisableSslSupport()));
        }
        settings.applyToClusterSettings(new ClusterSettingBuilder(config));
        settings.applyToConnectionPoolSettings(
                new ConnectionPoolSettingsBuilder(config, mongoClientSupport.getConnectionPoolListeners()));
        settings.applyToServerSettings(new ServerSettingsBuilder(config));
        settings.applyToSocketSettings(new SocketSettingsBuilder(config));

        if (config.readPreference.isPresent()) {
            settings.readPreference(ReadPreference.valueOf(config.readPreference.get()));
        }
        if (config.readConcern.isPresent()) {
            settings.readConcern(new ReadConcern(ReadConcernLevel.fromString(config.readConcern.get())));
        }

        return settings.build();
    }

    private void configureCodecRegistry(CodecRegistry defaultCodecRegistry, MongoClientSettings.Builder settings) {
        List<CodecProvider> providers = new ArrayList<>();
        for (CodecProvider codecProvider : codecProviders) {
            providers.add(codecProvider);
        }

        // add pojo codec provider with automatic capabilities
        // it always needs to be the last codec provided
        PojoCodecProvider.Builder pojoCodecProviderBuilder = PojoCodecProvider.builder()
                .automatic(true)
                .conventions(Conventions.DEFAULT_CONVENTIONS);
        // register bson discriminators
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String bsonDiscriminator : mongoClientSupport.getBsonDiscriminators()) {
            try {
                pojoCodecProviderBuilder
                        .register(ClassModel.builder(Class.forName(bsonDiscriminator, true, classLoader))
                                .enableDiscriminator(true).build());
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
        // register property codec provider
        for (PropertyCodecProvider propertyCodecProvider : propertyCodecProviders) {
            pojoCodecProviderBuilder.register(propertyCodecProvider);
        }

        CodecRegistry registry = !providers.isEmpty() ? fromRegistries(fromProviders(providers), defaultCodecRegistry,
                fromProviders(pojoCodecProviderBuilder.build()))
                : fromRegistries(defaultCodecRegistry, fromProviders(pojoCodecProviderBuilder.build()));
        settings.codecRegistry(registry);
    }

    private static List<ServerAddress> parseHosts(List<String> addresses) {
        if (addresses.isEmpty()) {
            return Collections.singletonList(new ServerAddress(ServerAddress.defaultHost(), ServerAddress.defaultPort()));
        }

        return addresses.stream()
                .map(String::trim)
                .map(new addressParser()).collect(Collectors.toList());
    }

    private static class addressParser implements Function<String, ServerAddress> {
        @Override
        public ServerAddress apply(String address) {
            String[] segments = COLON_PATTERN.split(address);
            if (segments.length == 1) {
                // Host only, default port
                return new ServerAddress(address);
            } else if (segments.length == 2) {
                // Host and port
                return new ServerAddress(segments[0], Integer.parseInt(segments[1]));
            } else {
                throw new IllegalArgumentException("Invalid server address " + address);
            }
        }
    }

    private MongoCredential createMongoCredential(MongoClientConfig config) {
        UsernamePassword usernamePassword = determineUserNamePassword(config.credentials);
        if (usernamePassword == null) {
            return null;
        }

        // get the authsource, or the database from the config, or 'admin' as it is the default auth source in mongo
        // and null is not allowed
        String authSource = config.credentials.authSource.orElse(config.database.orElse("admin"));
        // AuthMechanism
        AuthenticationMechanism mechanism = null;
        Optional<String> maybeMechanism = config.credentials.authMechanism;
        if (maybeMechanism.isPresent()) {
            mechanism = getAuthenticationMechanism(maybeMechanism.get());
        }

        // Create the MongoCredential instance.
        String username = usernamePassword.getUsername();
        char[] password = usernamePassword.getPassword();
        MongoCredential credential;
        if (mechanism == GSSAPI) {
            credential = MongoCredential.createGSSAPICredential(username);
        } else if (mechanism == PLAIN) {
            credential = MongoCredential.createPlainCredential(username, authSource, password);
        } else if (mechanism == MONGODB_X509) {
            credential = MongoCredential.createMongoX509Credential(username);
        } else if (mechanism == SCRAM_SHA_1) {
            credential = MongoCredential.createScramSha1Credential(username, authSource, password);
        } else if (mechanism == SCRAM_SHA_256) {
            credential = MongoCredential.createScramSha256Credential(username, authSource, password);
        } else if (mechanism == MONGODB_AWS) {
            credential = MongoCredential.createAwsCredential(username, password);
        } else if (mechanism == null) {
            credential = MongoCredential.createCredential(username, authSource, password);
        } else {
            throw new IllegalArgumentException("Unsupported authentication mechanism " + mechanism);
        }

        //add the properties
        if (!config.credentials.authMechanismProperties.isEmpty()) {
            for (Map.Entry<String, String> entry : config.credentials.authMechanismProperties.entrySet()) {
                credential = credential.withMechanismProperty(entry.getKey(), entry.getValue());
            }
        }

        return credential;
    }

    private UsernamePassword determineUserNamePassword(CredentialConfig config) {
        if (config.credentialsProvider.isPresent()) {
            String beanName = config.credentialsProviderName.orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = config.credentialsProvider.get();
            Map<String, String> credentials = credentialsProvider.getCredentials(name);
            String user = credentials.get(USER_PROPERTY_NAME);
            String password = credentials.get(PASSWORD_PROPERTY_NAME);
            return new UsernamePassword(user, password.toCharArray());
        } else {
            String username = config.username.orElse(null);
            if (username == null) {
                return null;
            }
            char[] password = config.password.map(String::toCharArray).orElse(null);
            return new UsernamePassword(username, password);
        }
    }

    private AuthenticationMechanism getAuthenticationMechanism(String authMechanism) {
        AuthenticationMechanism mechanism;
        try {
            mechanism = AuthenticationMechanism.fromMechanismName(authMechanism.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid authMechanism '" + authMechanism + "'");
        }
        return mechanism;
    }

    @PreDestroy
    public void stop() {
        for (MongoClient client : mongoclients.values()) {
            if (client != null) {
                client.close();
            }
        }
        for (ReactiveMongoClient reactive : reactiveMongoClients.values()) {
            if (reactive != null) {
                reactive.close();
            }
        }
    }

    private static class UsernamePassword {
        private final String username;
        private final char[] password;

        public UsernamePassword(String username, char[] password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public char[] getPassword() {
            return password;
        }
    }
}

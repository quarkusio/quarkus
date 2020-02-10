package io.quarkus.mongodb.runtime;

import static com.mongodb.AuthenticationMechanism.GSSAPI;
import static com.mongodb.AuthenticationMechanism.MONGODB_X509;
import static com.mongodb.AuthenticationMechanism.PLAIN;
import static com.mongodb.AuthenticationMechanism.SCRAM_SHA_1;

import java.lang.reflect.Constructor;
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

import javax.annotation.PreDestroy;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jboss.logging.Logger;

import com.mongodb.AuthenticationMechanism;
import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.ServerSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;

import io.quarkus.mongodb.ReactiveMongoClient;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;

public abstract class AbstractMongoClientProducer {

    private static final Logger LOGGER = Logger.getLogger(AbstractMongoClientProducer.class.getName());
    private static final Pattern COLON_PATTERN = Pattern.compile(":");
    private MongodbConfig mongodbConfig;
    private boolean disableSslSupport = false;
    private List<String> codecProviders;
    private List<String> bsonDiscriminators;
    private Map<String, MongoClient> mongoclients = new HashMap<>();
    private Map<String, ReactiveMongoClient> reactiveMongoClients = new HashMap<>();

    public MongoClientConfig getDefaultMongoClientConfig() {
        return mongodbConfig.defaultMongoClientConfig;
    }

    public MongoClient getClient(String name) {
        return mongoclients.get(name);
    }

    public ReactiveMongoClient getReactiveClient(String name) {
        return reactiveMongoClients.get(name);
    }

    public MongoClientConfig getMongoClientConfig(String clientName) {
        return mongodbConfig.mongoClientConfigs.get(clientName);
    }

    public MongoClient createMongoClient(MongoClientConfig mongoClientConfig, String name) throws MongoException {
        MongoClientSettings mongoConfiguration = createMongoConfiguration(mongoClientConfig);
        MongoClient client = MongoClients.create(mongoConfiguration);
        mongoclients.put(name, client);
        return client;
    }

    public ReactiveMongoClient createReactiveMongoClient(MongoClientConfig mongoClientConfig, String name)
            throws MongoException {
        MongoClientSettings mongoConfiguration = createMongoConfiguration(mongoClientConfig);
        ReactiveMongoClient reactive = new ReactiveMongoClientImpl(
                com.mongodb.reactivestreams.client.MongoClients.create(mongoConfiguration));
        reactiveMongoClients.put(name, reactive);
        return reactive;
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

            config.maxWaitQueueSize.ifPresent(builder::maxWaitQueueSize);
            config.replicaSetName.ifPresent(builder::requiredReplicaSetName);

            if (config.serverSelectionTimeout.isPresent()) {
                builder.serverSelectionTimeout(config.serverSelectionTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    private static class ConnectionPoolSettingsBuilder implements Block<ConnectionPoolSettings.Builder> {
        public ConnectionPoolSettingsBuilder(MongoClientConfig config) {
            this.config = config;
        }

        private MongoClientConfig config;

        @Override
        public void apply(ConnectionPoolSettings.Builder builder) {
            config.maxPoolSize.ifPresent(builder::maxSize);
            config.minPoolSize.ifPresent(builder::minSize);
            config.maxWaitQueueSize.ifPresent(builder::maxWaitQueueSize);
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
        checkCodec();
        CodecRegistry defaultCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();

        MongoClientSettings.Builder settings = MongoClientSettings.builder();

        ConnectionString connectionString;
        Optional<String> maybeConnectionString = config.connectionString;
        if (maybeConnectionString.isPresent()) {
            connectionString = new ConnectionString(maybeConnectionString.get());
            settings.applyConnectionString(connectionString);
        }

        List<CodecProvider> providers = new ArrayList<>();
        if (!codecProviders.isEmpty()) {
            providers.addAll(getCodecProviders(codecProviders));
        }
        // add pojo codec provider with automatic capabilities
        // it always needs to be the last codec provided
        PojoCodecProvider.Builder pojoCodecProviderBuilder = PojoCodecProvider.builder()
                .automatic(true)
                .conventions(Conventions.DEFAULT_CONVENTIONS);
        // register bson discriminators
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String bsonDiscriminator : bsonDiscriminators) {
            try {
                pojoCodecProviderBuilder
                        .register(ClassModel.builder(Class.forName(bsonDiscriminator, true, classLoader))
                                .enableDiscriminator(true).build());
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
        providers.add(pojoCodecProviderBuilder.build());
        CodecRegistry registry = CodecRegistries.fromRegistries(defaultCodecRegistry,
                CodecRegistries.fromProviders(providers));
        settings.codecRegistry(registry);

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
                concern = concern.withW(maybeW.get());
            }
            settings.writeConcern(concern);
            settings.retryWrites(wc.retryWrites);
        }
        if (config.tls) {
            settings.applyToSslSettings(new SslSettingsBuilder(config, disableSslSupport));
        }
        settings.applyToClusterSettings(new ClusterSettingBuilder(config));
        settings.applyToConnectionPoolSettings(new ConnectionPoolSettingsBuilder(config));
        settings.applyToServerSettings(new ServerSettingsBuilder(config));
        settings.applyToSocketSettings(new SocketSettingsBuilder(config));

        if (config.readPreference.isPresent()) {
            settings.readPreference(ReadPreference.valueOf(config.readPreference.get()));
        }
        return settings.build();
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
        String username = config.credentials.username.orElse(null);
        if (username == null) {
            return null;
        }

        char[] password = config.credentials.password.map(String::toCharArray).orElse(null);
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
        MongoCredential credential;
        if (mechanism == GSSAPI) {
            credential = MongoCredential.createGSSAPICredential(username);
        } else if (mechanism == PLAIN) {
            credential = MongoCredential.createPlainCredential(username, authSource, password);
        } else if (mechanism == MONGODB_X509) {
            credential = MongoCredential.createMongoX509Credential(username);
        } else if (mechanism == SCRAM_SHA_1) {
            credential = MongoCredential.createScramSha1Credential(username, authSource, password);
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

    private AuthenticationMechanism getAuthenticationMechanism(String authMechanism) {
        AuthenticationMechanism mechanism;
        try {
            mechanism = AuthenticationMechanism.fromMechanismName(authMechanism.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid authMechanism '" + authMechanism + "'");
        }
        return mechanism;
    }

    private List<CodecProvider> getCodecProviders(List<String> classNames) {
        List<CodecProvider> providers = new ArrayList<>();
        for (String name : classNames) {
            try {
                Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
                Constructor clazzConstructor = clazz.getConstructor();
                providers.add((CodecProvider) clazzConstructor.newInstance());
            } catch (Exception e) {
                LOGGER.warnf(e, "Unable to load the codec provider class %s", name);
            }
        }

        return providers;
    }

    public void setConfig(MongodbConfig config) {
        this.mongodbConfig = config;
    }

    public void setCodecs(List<String> codecs) {
        this.codecProviders = codecs;
    }

    public void setBsonDiscriminators(List<String> bsonDiscriminators) {
        this.bsonDiscriminators = bsonDiscriminators;
    }

    public void disableSslSupport() {
        this.disableSslSupport = true;
    }

    private void checkCodec() {
        if (codecProviders == null) {
            throw new IllegalStateException(
                    "The mongo clients are not ready to be consumed: the codec list for mongo configuration has not been injected yet");
        }
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
}

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

import java.lang.annotation.Annotation;
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
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Singleton;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.codecs.pojo.PropertyCodecProvider;

import com.mongodb.AuthenticationMechanism;
import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoConfigurationException;
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
import com.mongodb.connection.TransportSettings;
import com.mongodb.event.CommandListener;
import com.mongodb.event.ConnectionPoolListener;
import com.mongodb.reactivestreams.client.ReactiveContextProvider;

import io.netty.channel.socket.nio.NioSocketChannel;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.impl.VertxByteBufAllocator;

/**
 * This class is sort of a producer for {@link MongoClient} and {@link ReactiveMongoClient}.
 * <p>
 * It isn't a CDI producer in the literal sense, but it is marked as a bean
 * and its {@code createMongoClient} and {@code createReactiveMongoClient} methods are called at runtime in order to produce
 * the actual client objects.
 */
@Singleton
public class MongoClients {

    private static final Pattern COLON_PATTERN = Pattern.compile(":");

    private final MongodbConfig mongodbConfig;
    private final MongoClientSupport mongoClientSupport;
    private final Instance<CodecProvider> codecProviders;
    private final TlsConfigurationRegistry tlsConfigurationRegistry;
    private final Instance<PropertyCodecProvider> propertyCodecProviders;
    private final Instance<CommandListener> commandListeners;

    private final Map<String, MongoClient> mongoclients = new HashMap<>();
    private final Map<String, ReactiveMongoClient> reactiveMongoClients = new HashMap<>();
    private final Instance<ReactiveContextProvider> reactiveContextProviders;
    private final Instance<MongoClientCustomizer> customizers;
    private final Vertx vertx;

    public MongoClients(MongodbConfig mongodbConfig, MongoClientSupport mongoClientSupport,
            Instance<CodecProvider> codecProviders,
            TlsConfigurationRegistry tlsConfigurationRegistry,
            Instance<PropertyCodecProvider> propertyCodecProviders,
            Instance<CommandListener> commandListeners,
            Instance<ReactiveContextProvider> reactiveContextProviders,
            @Any Instance<MongoClientCustomizer> customizers,
            Vertx vertx) {
        this.mongodbConfig = mongodbConfig;
        this.mongoClientSupport = mongoClientSupport;
        this.codecProviders = codecProviders;
        this.tlsConfigurationRegistry = tlsConfigurationRegistry;
        this.propertyCodecProviders = propertyCodecProviders;
        this.commandListeners = commandListeners;
        this.reactiveContextProviders = reactiveContextProviders;
        this.customizers = customizers;
        this.vertx = vertx;

        try {
            //JDK bug workaround
            //https://github.com/quarkusio/quarkus/issues/14424
            //force class init to prevent possible deadlock when done by mongo threads
            Class.forName("sun.net.ext.ExtendedSocketOptions", true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException ignored) {
        }
    }

    public MongoClient createMongoClient(String clientName) throws MongoException {
        MongoClientSettings mongoConfiguration = createMongoConfiguration(clientName, getMatchingMongoClientConfig(clientName),
                false);
        MongoClient client = com.mongodb.client.MongoClients.create(mongoConfiguration);
        mongoclients.put(clientName, client);
        return client;
    }

    public ReactiveMongoClient createReactiveMongoClient(String clientName)
            throws MongoException {
        MongoClientSettings mongoConfiguration = createMongoConfiguration(clientName, getMatchingMongoClientConfig(clientName),
                true);
        com.mongodb.reactivestreams.client.MongoClient client = com.mongodb.reactivestreams.client.MongoClients
                .create(mongoConfiguration);
        ReactiveMongoClientImpl reactive = new ReactiveMongoClientImpl(client);
        reactiveMongoClients.put(clientName, reactive);
        return reactive;
    }

    public MongoClientConfig getMatchingMongoClientConfig(String clientName) {
        return MongoClientBeanUtil.isDefault(clientName) ? mongodbConfig.defaultMongoClientConfig()
                : mongodbConfig.mongoClientConfigs().get(clientName);
    }

    private static class ClusterSettingBuilder implements Block<ClusterSettings.Builder> {
        public ClusterSettingBuilder(MongoClientConfig config) {
            this.config = config;
        }

        private final MongoClientConfig config;

        @Override
        public void apply(ClusterSettings.Builder builder) {
            Optional<String> maybeConnectionString = config.connectionString();
            if (maybeConnectionString.isEmpty()) {
                // Parse hosts
                List<ServerAddress> hosts = parseHosts(config.hosts());
                builder.hosts(hosts);

                if (hosts.size() == 1 && config.replicaSetName().isEmpty()) {
                    builder.mode(ClusterConnectionMode.SINGLE);
                } else {
                    builder.mode(ClusterConnectionMode.MULTIPLE);
                }
            }
            if (config.localThreshold().isPresent()) {
                builder.localThreshold(config.localThreshold().get().toMillis(), TimeUnit.MILLISECONDS);
            }

            config.replicaSetName().ifPresent(builder::requiredReplicaSetName);

            if (config.serverSelectionTimeout().isPresent()) {
                builder.serverSelectionTimeout(config.serverSelectionTimeout().get().toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    private static class ConnectionPoolSettingsBuilder implements Block<ConnectionPoolSettings.Builder> {
        public ConnectionPoolSettingsBuilder(MongoClientConfig config, List<ConnectionPoolListener> connectionPoolListeners) {
            this.config = config;
            this.connectionPoolListeners = connectionPoolListeners;
        }

        private final MongoClientConfig config;
        private final List<ConnectionPoolListener> connectionPoolListeners;

        @Override
        public void apply(ConnectionPoolSettings.Builder builder) {
            config.maxPoolSize().ifPresent(builder::maxSize);
            config.minPoolSize().ifPresent(builder::minSize);
            if (config.maxConnectionIdleTime().isPresent()) {
                builder.maxConnectionIdleTime(config.maxConnectionIdleTime().get().toMillis(), TimeUnit.MILLISECONDS);
            }
            if (config.maxConnectionLifeTime().isPresent()) {
                builder.maxConnectionLifeTime(config.maxConnectionLifeTime().get().toMillis(), TimeUnit.MILLISECONDS);
            }
            if (config.maintenanceFrequency().isPresent()) {
                builder.maintenanceFrequency(config.maintenanceFrequency().get().toMillis(), TimeUnit.MILLISECONDS);
            }
            if (config.maintenanceInitialDelay().isPresent()) {
                builder.maintenanceInitialDelay(config.maintenanceInitialDelay().get().toMillis(), TimeUnit.MILLISECONDS);
            }
            for (ConnectionPoolListener connectionPoolListener : connectionPoolListeners) {
                builder.addConnectionPoolListener(connectionPoolListener);
            }
        }
    }

    private static class SslSettingsBuilder implements Block<SslSettings.Builder> {
        private final TlsConfigurationRegistry tlsConfigurationRegistry;

        private final MongoClientConfig config;
        private final boolean disableSslSupport;

        public SslSettingsBuilder(MongoClientConfig config,
                TlsConfigurationRegistry tlsConfigurationRegistry,
                boolean disableSslSupport) {
            this.config = config;
            this.disableSslSupport = disableSslSupport;
            this.tlsConfigurationRegistry = tlsConfigurationRegistry;
        }

        @Override
        public void apply(SslSettings.Builder builder) {
            builder.enabled(!disableSslSupport).invalidHostNameAllowed(config.tlsInsecure());
            if (!disableSslSupport) {
                Optional<TlsConfiguration> tlsConfig = TlsConfiguration.from(tlsConfigurationRegistry,
                        config.tlsConfigurationName());
                if (tlsConfig.isPresent()) {
                    try {
                        builder.context(tlsConfig.get().createSSLContext());
                    } catch (Exception e) {
                        throw new MongoConfigurationException("Could not configure MongoDB client with TLS registry", e);
                    }
                }
            }
        }
    }

    private static class SocketSettingsBuilder implements Block<SocketSettings.Builder> {
        public SocketSettingsBuilder(MongoClientConfig config) {
            this.config = config;
        }

        private final MongoClientConfig config;

        @Override
        public void apply(SocketSettings.Builder builder) {
            if (config.connectTimeout().isPresent()) {
                builder.connectTimeout((int) config.connectTimeout().get().toMillis(), TimeUnit.MILLISECONDS);
            }
            if (config.readTimeout().isPresent()) {
                builder.readTimeout((int) config.readTimeout().get().toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    private static class ServerSettingsBuilder implements Block<ServerSettings.Builder> {
        public ServerSettingsBuilder(MongoClientConfig config) {
            this.config = config;
        }

        private final MongoClientConfig config;

        @Override
        public void apply(ServerSettings.Builder builder) {
            if (config.heartbeatFrequency().isPresent()) {
                builder.heartbeatFrequency((int) config.heartbeatFrequency().get().toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    private MongoClientSettings createMongoConfiguration(String name, MongoClientConfig config, boolean isReactive) {
        if (config == null) {
            throw new RuntimeException("mongo config is missing for creating mongo client.");
        }
        CodecRegistry defaultCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();

        MongoClientSettings.Builder settings = MongoClientSettings.builder();

        if (isReactive) {
            switch (config.reactiveTransport()) {
                case NETTY:
                    // we supports just NIO for now
                    if (!vertx.isNativeTransportEnabled()) {
                        configureNettyTransport(settings);
                    }
                    break;
                case MONGO:
                    // no-op since this is the default behaviour
                    break;
            }
            reactiveContextProviders.stream().findAny().ifPresent(settings::contextProvider);
        }

        ConnectionString connectionString;
        Optional<String> maybeConnectionString = config.connectionString();
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

        config.applicationName().ifPresent(settings::applicationName);

        if (config.credentials() != null) {
            MongoCredential credential = createMongoCredential(config);
            if (credential != null) {
                settings.credential(credential);
            }
        }

        if (config.writeConcern() != null) {
            WriteConcernConfig wc = config.writeConcern();
            WriteConcern concern = (wc.safe() ? WriteConcern.ACKNOWLEDGED : WriteConcern.UNACKNOWLEDGED)
                    .withJournal(wc.journal());

            if (wc.wTimeout().isPresent()) {
                concern = concern.withWTimeout(wc.wTimeout().get().toMillis(), TimeUnit.MILLISECONDS);
            }

            Optional<String> maybeW = wc.w();
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
            settings.retryWrites(wc.retryWrites());
        }
        if (config.tls() || config.tlsConfigurationName().isPresent()) {
            settings.applyToSslSettings(new SslSettingsBuilder(config,
                    tlsConfigurationRegistry,
                    mongoClientSupport.isDisableSslSupport()));
        }
        settings.applyToClusterSettings(new ClusterSettingBuilder(config));
        settings.applyToConnectionPoolSettings(
                new ConnectionPoolSettingsBuilder(config, mongoClientSupport.getConnectionPoolListeners()));
        settings.applyToServerSettings(new ServerSettingsBuilder(config));
        settings.applyToSocketSettings(new SocketSettingsBuilder(config));

        if (config.readPreference().isPresent()) {
            settings.readPreference(ReadPreference.valueOf(config.readPreference().get()));
        }
        if (config.readConcern().isPresent()) {
            settings.readConcern(new ReadConcern(ReadConcernLevel.fromString(config.readConcern().get())));
        }

        if (config.uuidRepresentation().isPresent()) {
            settings.uuidRepresentation(config.uuidRepresentation().get());
        }

        settings = customize(name, settings);

        return settings.build();
    }

    private void configureNettyTransport(MongoClientSettings.Builder settings) {
        var nettyStreaming = TransportSettings.nettyBuilder()
                .allocator(VertxByteBufAllocator.POOLED_ALLOCATOR)
                .eventLoopGroup(vertx.nettyEventLoopGroup())
                .socketChannelClass(NioSocketChannel.class).build();
        settings.transportSettings(nettyStreaming);
    }

    private boolean doesNotHaveClientNameQualifier(Bean<?> bean) {
        for (Annotation qualifier : bean.getQualifiers()) {
            if (qualifier.annotationType().equals(MongoClientName.class)) {
                return false;
            }
        }
        return true;
    }

    private MongoClientSettings.Builder customize(String name, MongoClientSettings.Builder settings) {
        // If the client name is the default one, we use a customizer that does not have the MongoClientName qualifier.
        // Otherwise, we use the one that has the qualifier.
        // Note that at build time, we check that we have at most one customizer per client, including for the default one.
        if (MongoClientBeanUtil.isDefault(name)) {
            var maybe = customizers.handlesStream()
                    .filter(h -> doesNotHaveClientNameQualifier(h.getBean()))
                    .findFirst(); // We have at most one customizer without the qualifier.
            if (maybe.isEmpty()) {
                return settings;
            } else {
                return maybe.get().get().customize(settings);
            }
        } else {
            Instance<MongoClientCustomizer> selected = customizers.select(MongoClientName.Literal.of(name));
            if (selected.isResolvable()) { // We can use resolvable, as we have at most one customizer per client
                return selected.get().customize(settings);
            }
            return settings;
        }
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

        // get the authsource, or the database from the config, or 'admin' as it is the default auth source in mongo
        // and null is not allowed
        String authSource = config.credentials().authSource().orElse(config.database().orElse("admin"));
        // AuthMechanism
        AuthenticationMechanism mechanism = null;
        Optional<String> maybeMechanism = config.credentials().authMechanism();
        if (maybeMechanism.isPresent()) {
            mechanism = getAuthenticationMechanism(maybeMechanism.get());
        }

        UsernamePassword usernamePassword = determineUserNamePassword(config.credentials());
        if (usernamePassword == null) {
            if (mechanism == null) {
                return null;
            }
            usernamePassword = new UsernamePassword(null, null);
        }
        // Create the MongoCredential instance.
        String username = usernamePassword.username();
        char[] password = usernamePassword.password();
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
        if (!config.credentials().authMechanismProperties().isEmpty()) {
            for (Map.Entry<String, String> entry : config.credentials().authMechanismProperties().entrySet()) {
                credential = credential.withMechanismProperty(entry.getKey(), entry.getValue());
            }
        }

        return credential;
    }

    private UsernamePassword determineUserNamePassword(CredentialConfig config) {
        if (config.credentialsProvider().isPresent()) {
            String beanName = config.credentialsProviderName().orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = config.credentialsProvider().get();
            Map<String, String> credentials = credentialsProvider.getCredentials(name);
            String user = credentials.get(USER_PROPERTY_NAME);
            String password = credentials.get(PASSWORD_PROPERTY_NAME);
            return new UsernamePassword(user, password.toCharArray());
        } else {
            String username = config.username().orElse(null);
            if (username == null) {
                return null;
            }
            char[] password = config.password().map(String::toCharArray).orElse(null);
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

    private record UsernamePassword(String username, char[] password) {
    }
}

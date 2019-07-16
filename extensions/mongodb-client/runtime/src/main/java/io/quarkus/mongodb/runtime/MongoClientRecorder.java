package io.quarkus.mongodb.runtime;

import static com.mongodb.AuthenticationMechanism.GSSAPI;
import static com.mongodb.AuthenticationMechanism.MONGODB_X509;
import static com.mongodb.AuthenticationMechanism.PLAIN;
import static com.mongodb.AuthenticationMechanism.SCRAM_SHA_1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jboss.logging.Logger;

import com.mongodb.AuthenticationMechanism;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ClusterConnectionMode;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.mongodb.ReactiveMongoClient;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MongoClientRecorder {

    private static final Logger LOGGER = Logger.getLogger(MongoClientRecorder.class);

    private static volatile MongoClient client;
    private static volatile ReactiveMongoClient reactiveMongoClient;

    public RuntimeValue<MongoClient> configureTheClient(
            MongoClientConfig config,
            BeanContainer container,
            LaunchMode launchMode, ShutdownContext shutdown,
            List<String> codecProviders) {
        initialize(config, codecProviders);

        MongoClientProducer producer = container.instance(MongoClientProducer.class);
        producer.initialize(client, reactiveMongoClient);

        if (!launchMode.isDevOrTest()) {
            shutdown.addShutdownTask(this::close);
        }
        return new RuntimeValue<>(client);
    }

    public RuntimeValue<ReactiveMongoClient> configureTheReactiveClient() {
        return new RuntimeValue<>(reactiveMongoClient);
    }

    private void close() {
        if (client != null) {
            client.close();
        }
        if (reactiveMongoClient != null) {
            reactiveMongoClient.close();
        }
    }

    void initialize(MongoClientConfig config, List<String> codecProviders) {
        CodecRegistry defaultCodecRegistry = com.mongodb.MongoClient.getDefaultCodecRegistry();

        MongoClientSettings.Builder settings = MongoClientSettings.builder();

        ConnectionString connectionString;
        Optional<String> maybeConnectionString = config.connectionString;
        if (maybeConnectionString.isPresent()) {
            connectionString = new ConnectionString(maybeConnectionString.get());
            settings.applyConnectionString(connectionString);
        }

        CodecRegistry registry = defaultCodecRegistry;
        if (!codecProviders.isEmpty()) {
            registry = CodecRegistries.fromRegistries(defaultCodecRegistry,
                    CodecRegistries.fromProviders(getCodecProviders(codecProviders)));
        }
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
            settings.applyToSslSettings(builder -> builder.enabled(true).invalidHostNameAllowed(config.tlsInsecure));
        }

        settings.applyToClusterSettings(builder -> {
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
            config.localThreshold.ifPresent(i -> builder.localThreshold(i.toMillis(), TimeUnit.MILLISECONDS));
            config.maxWaitQueueSize.ifPresent(builder::maxWaitQueueSize);
            config.replicaSetName.ifPresent(builder::requiredReplicaSetName);
            config.serverSelectionTimeout.ifPresent(i -> builder.serverSelectionTimeout(i.toMillis(), TimeUnit.MILLISECONDS));
        });

        settings.applyToConnectionPoolSettings(builder -> {
            config.maxPoolSize.ifPresent(builder::maxSize);
            config.minPoolSize.ifPresent(builder::minSize);
            config.maxWaitQueueSize.ifPresent(builder::maxWaitQueueSize);
            config.maxConnectionIdleTime.ifPresent(i -> builder.maxConnectionIdleTime(i.toMillis(), TimeUnit.MILLISECONDS));
            config.maxConnectionLifeTime.ifPresent(i -> builder.maxConnectionLifeTime(i.toMillis(), TimeUnit.MILLISECONDS));
            config.maintenanceFrequency.ifPresent(i -> builder.maintenanceFrequency(i.toMillis(), TimeUnit.MILLISECONDS));
            config.maintenanceInitialDelay.ifPresent(i -> builder.maintenanceInitialDelay(i.toMillis(), TimeUnit.MILLISECONDS));
        });

        settings.applyToServerSettings(
                builder -> config.heartbeatFrequency
                        .ifPresent(i -> builder.heartbeatFrequency(i.toMillis(), TimeUnit.MILLISECONDS)));

        settings.applyToSocketSettings(builder -> config.connectTimeout
                .ifPresent(i -> builder.connectTimeout((int) i.toMillis(), TimeUnit.MILLISECONDS)));

        config.readPreference.ifPresent(pref -> settings.readPreference(ReadPreference.valueOf(pref)));

        MongoClientSettings mongoConfiguration = settings.build();
        client = MongoClients.create(mongoConfiguration);
        reactiveMongoClient = new ReactiveMongoClientImpl(
                com.mongodb.reactivestreams.client.MongoClients.create(mongoConfiguration));
    }

    List<CodecProvider> getCodecProviders(List<String> classNames) {
        List<CodecProvider> providers = new ArrayList<>();
        for (String name : classNames) {
            try {
                Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
                providers.add((CodecProvider) clazz.newInstance());
            } catch (Exception e) {
                LOGGER.warnf("Unable to load the codec provider class %s", name, e);
            }
        }
        return providers;
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

    private static List<ServerAddress> parseHosts(List<String> addresses) {
        if (addresses.isEmpty()) {
            return Collections.singletonList(new ServerAddress(ServerAddress.defaultHost(), ServerAddress.defaultPort()));
        }

        return addresses.stream()
                .map(String::trim)
                .map(address -> {
                    String[] segments = address.split(":");
                    if (segments.length == 1) {
                        // Host only, default port
                        return new ServerAddress(address);
                    } else if (segments.length == 2) {
                        // Host and port
                        return new ServerAddress(segments[0], Integer.parseInt(segments[1]));
                    } else {
                        throw new IllegalArgumentException("Invalid server address " + address);
                    }
                }).collect(Collectors.toList());
    }
}

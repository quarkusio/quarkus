package io.quarkus.mongo.runtime;

import static com.mongodb.AuthenticationMechanism.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ClusterConnectionMode;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.mongo.ReactiveMongoClient;
import io.quarkus.mongo.impl.ReactiveMongoClientImpl;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;

@Template
public class MongoClientTemplate {

    private static volatile MongoClient client;
    private static volatile ReactiveMongoClient reactiveMongoClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoClientTemplate.class);

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
        if (config.connectionString.isPresent()) {
            connectionString = new ConnectionString(config.connectionString.get());
            settings.applyConnectionString(connectionString);
        }

        CodecRegistry registry = defaultCodecRegistry;
        if (!codecProviders.isEmpty()) {
            registry = CodecRegistries.fromRegistries(defaultCodecRegistry,
                    CodecRegistries.fromProviders(getCodecProviders(codecProviders)));
        }
        settings.codecRegistry(registry);

        // TODO Redo read preferences

        config.applicationName.ifPresent(settings::applicationName);

        if (config.credentials != null) {
            MongoCredential credential = createMongoCredential(config.credentials);
            if (credential != null) {
                settings.credential(credential);
            }
        }

        if (config.writeConcern != null) {
            WriteConcernConfig wc = config.writeConcern;
            WriteConcern concern = (wc.safe ? WriteConcern.ACKNOWLEDGED : WriteConcern.UNACKNOWLEDGED)
                    .withJournal(wc.journal);

            if (wc.wTimeout.isPresent()) {
                concern = concern.withWTimeout(wc.wTimeout.getAsInt(), TimeUnit.MILLISECONDS);
            }

            if (wc.w.isPresent()) {
                concern = concern.withW(wc.w.get());
            }
            settings.writeConcern(concern);
            settings.retryWrites(wc.retryWrites);
        }

        if (config.tls) {
            settings.applyToSslSettings(builder -> builder.enabled(true).invalidHostNameAllowed(config.tlsInsecure));
        }

        settings.applyToClusterSettings(builder -> {
            if (!config.connectionString.isPresent()) {
                // Parse hosts
                List<ServerAddress> hosts = parseHosts(config.hosts);
                builder.hosts(hosts);

                if (hosts.size() == 1 && !config.replicaSetName.isPresent()) {
                    builder.mode(ClusterConnectionMode.SINGLE);
                } else {
                    builder.mode(ClusterConnectionMode.MULTIPLE);
                }
            }
            config.localThreshold.ifPresent(i -> builder.localThreshold(i, TimeUnit.MILLISECONDS));
            config.maxWaitQueueSize.ifPresent(builder::maxWaitQueueSize);
            config.replicaSetName.ifPresent(builder::requiredReplicaSetName);
            config.serverSelectionTimeout.ifPresent(i -> builder.serverSelectionTimeout(i, TimeUnit.MILLISECONDS));
        });

        settings.applyToConnectionPoolSettings(builder -> {
            config.maxPoolSize.ifPresent(builder::maxSize);
            config.minPoolSize.ifPresent(builder::minSize);
            config.maxWaitQueueSize.ifPresent(builder::maxWaitQueueSize);
            config.maxConnectionIdleTime.ifPresent(i -> builder.maxConnectionIdleTime(i, TimeUnit.MILLISECONDS));
            config.maxConnectionLifeTime.ifPresent(i -> builder.maxConnectionLifeTime(i, TimeUnit.MILLISECONDS));
            config.maintenanceFrequency.ifPresent(i -> builder.maintenanceFrequency(i, TimeUnit.MILLISECONDS));
            config.maintenanceInitialDelay.ifPresent(i -> builder.maintenanceInitialDelay(i, TimeUnit.MILLISECONDS));
        });

        settings.applyToServerSettings(
                builder -> config.heartbeatFrequency.ifPresent(i -> builder.heartbeatFrequency(i, TimeUnit.MILLISECONDS)));

        settings.applyToSocketSettings(builder -> {
            config.connectTimeout.ifPresent(i -> builder.connectTimeout(i, TimeUnit.MILLISECONDS));
        });

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
                LOGGER.warn("Unable to load the codec provider class {} ", name, e);
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

    private MongoCredential createMongoCredential(CredentialConfig config) {
        String username = config.username.orElse(null);
        if (username == null) {
            return null;
        }

        char[] password = config.password.map(String::toCharArray).orElse(null);
        //admin is the default auth source in mongo and null is not allowed
        //TODO we should add a 'database' props and default to this one if the authSource is not set,
        // this is the standard Mongo behaviour. We can then default to admin ...
        String authSource = config.authSource.orElse("admin");
        // AuthMechanism
        AuthenticationMechanism mechanism = null;
        if (config.authMechanism.isPresent()) {
            mechanism = getAuthenticationMechanism(config.authMechanism.get());
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
        if (!config.authMechanismProperties.isEmpty()) {
            for (Map.Entry<String, String> entry : config.authMechanismProperties.entrySet()) {
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

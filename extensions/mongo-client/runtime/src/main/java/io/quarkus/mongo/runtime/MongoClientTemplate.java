package io.quarkus.mongo.runtime;

import static com.mongodb.AuthenticationMechanism.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;

@Template
public class MongoClientTemplate {

    private static volatile MongoClient client;
    // Use for the Vert.x client.
    // TODO How to inject it into the Vert.x client?
    private static volatile com.mongodb.async.client.MongoClient asyncClient;

    public RuntimeValue<MongoClient> configureTheClient(MongoClientConfig config,
            BeanContainer container,
            LaunchMode launchMode, ShutdownContext shutdown,
            List<String> codecProviders) {
        initialize(config, codecProviders);

        MongoClientProducer producer = container.instance(MongoClientProducer.class);
        producer.initialize(client, asyncClient, null);

        if (!launchMode.isDevOrTest()) {
            shutdown.addShutdownTask(this::close);
        }
        return new RuntimeValue<>(client);
    }

    private void close() {
        if (client != null) {
            client.close();
        }
    }

    void initialize(MongoClientConfig config, List<String> codecProviders) {
        if (client != null) {
            // Already configured
            return;
        }

        CodecRegistry defaultCodecRegistry = com.mongodb.MongoClient.getDefaultCodecRegistry();

        MongoClientSettings.Builder settings = MongoClientSettings.builder();

        ConnectionString connectionString = new ConnectionString(config.connectionString);
        settings.applyConnectionString(connectionString);

        CodecRegistry registry = defaultCodecRegistry;
        if (!codecProviders.isEmpty()) {
            registry = CodecRegistries.fromRegistries(registry,
                    CodecRegistries.fromProviders(getCodecProviders(codecProviders)));
        }
        settings.codecRegistry(registry);

        // TODO Redo read preferences

        config.applicationName.ifPresent(settings::applicationName);

        config.credentials.ifPresent(cc -> {
            MongoCredential credential = createMongoCredential(cc);
            if (credential != null) {
                settings.credential(credential);
            }
        });

        if (config.writeConcern.isPresent()) {
            WriteConcernConfig wc = config.writeConcern.get();
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
            config.localThreshold.ifPresent(i -> builder.localThreshold(i, TimeUnit.MILLISECONDS));
            config.maxWaitQueueSize.ifPresent(builder::maxWaitQueueSize);
            config.replicaSetName.ifPresent(builder::requiredReplicaSetName);
            config.serverSelectionTimeout.ifPresent(i -> builder.serverSelectionTimeout(i, TimeUnit.MILLISECONDS));
        });

        settings.applyToConnectionPoolSettings(builder -> {
            config.maxPoolSize.ifPresent(builder::maxSize);
            config.maxWaitQueueSize.ifPresent(builder::maxWaitQueueSize);
            config.maxConnectionIdleTime.ifPresent(i -> builder.maxConnectionIdleTime(i, TimeUnit.MILLISECONDS));
            config.maxConnectionLifeTime.ifPresent(i -> builder.maxConnectionLifeTime(i, TimeUnit.MILLISECONDS));
        });

        settings.applyToServerSettings(
                builder -> config.heartbeatFrequency.ifPresent(i -> builder.heartbeatFrequency(i, TimeUnit.MILLISECONDS)));

        settings.applyToSocketSettings(builder -> {
            config.connectTimeout.ifPresent(i -> builder.connectTimeout(i, TimeUnit.MILLISECONDS));
        });

        config.credentials.ifPresent(credentials -> {
            MongoCredential credential = MongoCredential.createCredential(
                    credentials.username.orElse(null),
                    credentials.authSource.orElse(null),
                    credentials.password.map(String::toCharArray).orElse(null));

            if (credentials.authMechanism.isPresent()) {
                credential = credential.withMechanism(
                        AuthenticationMechanism.valueOf(credentials.authMechanism.get().toUpperCase()));
            }

            if (!credentials.authMechanismProperties.isEmpty()) {
                for (Map.Entry<String, String> entry : credentials.authMechanismProperties.entrySet()) {
                    credential = credential.withMechanismProperty(entry.getKey(), entry.getValue());
                }
            }
        });

        //TODO Configure the Vert.x client
        client = MongoClients.create(settings.build());
        asyncClient = com.mongodb.async.client.MongoClients.create(settings.build());
    }

    List<? extends Codec<?>> getCodecs(List<String> classNames) {
        List<Codec<?>> codecs = new ArrayList<>();
        for (String name : classNames) {
            try {
                Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
                Codec codec = (Codec) clazz.newInstance();
                codecs.add(codec);
            } catch (Exception e) {
                // TODO LOG ME
                e.printStackTrace();
            }
        }
        return codecs;
    }

    List<CodecProvider> getCodecProviders(List<String> classNames) {
        List<CodecProvider> providers = new ArrayList<>();
        for (String name : classNames) {
            try {
                Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
                providers.add((CodecProvider) clazz.newInstance());
            } catch (Exception e) {
                // TODO LOG ME
                e.printStackTrace();
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
        } else {
            char[] password = config.password.map(String::toCharArray).orElse(null);
            String authSource = config.authSource.orElse(null);
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
            return credential;
        }
    }
}

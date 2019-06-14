package io.quarkus.mongo.runtime;

import java.util.ArrayList;
import java.util.List;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
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

    public RuntimeValue<MongoClient> configureTheClient(MongoClientConfig config,
            BeanContainer container,
            LaunchMode launchMode, ShutdownContext shutdown,
            List<String> codecProviders, List<String> codecs) {
        initialize(config, codecProviders, codecs);

        MongoClientProducer producer = container.instance(MongoClientProducer.class);
        producer.initialize(client, null);

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

    void initialize(MongoClientConfig config, List<String> codecs, List<String> codecProviders) {
        if (client != null) {
            // Already configured
            return;
        }

        CodecRegistry defaultCodecRegistry = com.mongodb.MongoClient.getDefaultCodecRegistry();

        MongoClientSettings.Builder settings = MongoClientSettings.builder();
        settings.applyConnectionString(new ConnectionString(config.connectionString));

        System.out.println("Codecs: " + codecs + " / " + codecProviders);
        CodecRegistry registry = defaultCodecRegistry;
        if (!codecs.isEmpty()) {
            registry = CodecRegistries.fromRegistries(registry, CodecRegistries.fromCodecs(getCodecs(codecs)));
        }
        if (!codecProviders.isEmpty()) {
            registry = CodecRegistries.fromRegistries(registry,
                    CodecRegistries.fromProviders(getCodecProviders(codecProviders)));
        }
        settings.codecRegistry(registry);

        //TODO Configure the Vert.x client

        client = MongoClients.create(settings.build());
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
}

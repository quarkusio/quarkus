package io.quarkus.mongo.runtime;

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
            LaunchMode launchMode, ShutdownContext shutdown) {
        initialize(config);

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

    void initialize(MongoClientConfig config) {
        if (client != null) {
            // Already configured
            return;
        }
        //TODO Configure the Vert.x client
        client = MongoClients.create(config.connectionString);
    }
}

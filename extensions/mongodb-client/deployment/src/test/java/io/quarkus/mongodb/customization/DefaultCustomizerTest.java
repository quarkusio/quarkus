package io.quarkus.mongodb.customization;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;

import io.quarkus.arc.ClientProxy;
import io.quarkus.mongodb.MongoTestBase;
import io.quarkus.mongodb.runtime.MongoClientCustomizer;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultCustomizerTest extends MongoTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class, MyCustomizer.class))
            .withConfigurationResource("default-mongoclient.properties");

    @Inject
    MongoClient client;

    @Test
    void testCustomizationOnDefaultConnection() {
        MongoClientImpl clientImpl = (MongoClientImpl) ClientProxy.unwrap(client);
        Assertions.assertThat(clientImpl.getSettings().getApplicationName()).isEqualTo("my-app");
    }

    @ApplicationScoped
    public static class MyCustomizer implements MongoClientCustomizer {

        @Override
        public MongoClientSettings.Builder customize(MongoClientSettings.Builder builder) {
            return builder.applicationName("my-app");
        }
    }
}

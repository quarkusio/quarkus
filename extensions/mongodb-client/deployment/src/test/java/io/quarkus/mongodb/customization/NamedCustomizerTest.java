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
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.MongoTestBase;
import io.quarkus.mongodb.runtime.MongoClientCustomizer;
import io.quarkus.test.QuarkusUnitTest;

public class NamedCustomizerTest extends MongoTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class, MyCustomizer.class))
            .withConfigurationResource("named-mongoclient.properties");

    @Inject
    MongoClient client;

    @Inject
    @MongoClientName("second")
    MongoClient secondClient;

    @Test
    void testCustomizationOnTwoConnections() {
        MongoClientImpl clientImpl = (MongoClientImpl) ClientProxy.unwrap(client);
        MongoClientImpl secondClientImpl = (MongoClientImpl) ClientProxy.unwrap(secondClient);
        Assertions.assertThat(clientImpl.getSettings().getApplicationName()).isEqualTo("my-app");
        Assertions.assertThat(secondClientImpl.getSettings().getApplicationName()).isEqualTo("my-second-app");
    }

    @ApplicationScoped
    public static class MyCustomizer implements MongoClientCustomizer {

        @Override
        public MongoClientSettings.Builder customize(MongoClientSettings.Builder builder) {
            return builder.applicationName("my-app");
        }
    }

    @ApplicationScoped
    @MongoClientName("second")
    public static class MySecondCustomizer implements MongoClientCustomizer {

        @Override
        public MongoClientSettings.Builder customize(MongoClientSettings.Builder builder) {
            return builder.applicationName("my-second-app");
        }
    }
}

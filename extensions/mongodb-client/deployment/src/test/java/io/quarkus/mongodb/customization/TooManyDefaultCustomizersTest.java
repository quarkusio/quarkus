package io.quarkus.mongodb.customization;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;

import io.quarkus.mongodb.MongoTestBase;
import io.quarkus.mongodb.runtime.MongoClientCustomizer;
import io.quarkus.test.QuarkusUnitTest;

public class TooManyDefaultCustomizersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class, MyCustomizer.class, MySecondCustomizer.class))
            .withConfigurationResource("default-mongoclient.properties")
            .assertException(t -> Assertions.assertThat(t).isInstanceOf(DeploymentException.class)
                    .hasMessageContaining("Multiple Mongo client customizers found for client <default>: "));

    @Inject
    MongoClient client;

    @Test
    void test() {
        fail("Should not be run");
    }

    @ApplicationScoped
    public static class MyCustomizer implements MongoClientCustomizer {

        @Override
        public MongoClientSettings.Builder customize(MongoClientSettings.Builder builder) {
            return builder.applicationName("my-app");
        }
    }

    @ApplicationScoped
    public static class MySecondCustomizer implements MongoClientCustomizer {

        @Override
        public MongoClientSettings.Builder customize(MongoClientSettings.Builder builder) {
            return builder.applicationName("my-second-app");
        }
    }
}

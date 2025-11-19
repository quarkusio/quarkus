package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusUnitTest;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Flapdoodle doesn't work very well on Windows with replicas")
public class MongoActiveClientsMissingConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class))
            .overrideRuntimeConfigKey("quarkus.mongodb.active.hosts", "")
            .overrideRuntimeConfigKey("quarkus.mongodb.active.connection-string", "")
            .assertException(e -> assertThat(e)// Can't use isInstanceOf due to weird classloading in tests
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            """
                                    Mongo Client 'active' was deactivated automatically because neither the \
                                    hosts nor the connectionString is set. \
                                    To activate the Mongo Client, set the configuration property 'quarkus.mongodb.active.hosts' \
                                    or 'quarkus.mongodb.active.connection-string' \
                                    Refer to https://quarkus.io/guides/mongodb for guidance.
                                    """));

    @Inject
    @MongoClientName("active")
    MongoClient mongoClient;
    @Inject
    @MongoClientName("active")
    ReactiveMongoClient reactiveMongoClient;

    @Test
    void missingConfig() {
        Assertions.fail();
    }
}

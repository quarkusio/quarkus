package io.quarkus.it.mongodb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.mongodb.client.MongoClient;

import io.quarkus.mongodb.MongoClientName;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class)
public class DevServicesStartTest {

    DevServicesContext context;

    @Inject
    @MongoClientName("with-connection-string")
    MongoClient clientWithConnectionString;

    @Inject
    @MongoClientName("with-hosts")
    MongoClient clientWithHosts;

    @ParameterizedTest
    @ValueSource(strings = {
            "quarkus.mongodb.with-connection-string.connection-string",
            "quarkus.mongodb.with-hosts.connection-string"
    })
    public void testDevServicesNotStarted(String property) {
        assertThat(context.devServicesProperties(), not(hasKey(property)));
    }

    @Test
    public void testDevServicesStarted() {
        assertThat(context.devServicesProperties(), hasKey("quarkus.mongodb.connection-string"));
    }
}

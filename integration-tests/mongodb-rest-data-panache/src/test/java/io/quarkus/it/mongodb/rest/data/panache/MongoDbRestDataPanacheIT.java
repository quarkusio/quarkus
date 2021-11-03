package io.quarkus.it.mongodb.rest.data.panache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@DisabledOnOs(OS.WINDOWS)
class MongoDbRestDataPanacheIT extends MongoDbRestDataPanacheTest {

    DevServicesContext context;

    @Test
    public void testDevServicesProperties() {
        assertThat(context.devServicesProperties()).hasSize(1).containsKey("quarkus.mongodb.connection-string");
    }
}

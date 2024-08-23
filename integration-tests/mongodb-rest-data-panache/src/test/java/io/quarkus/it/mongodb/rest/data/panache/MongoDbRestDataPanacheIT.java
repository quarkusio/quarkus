package io.quarkus.it.mongodb.rest.data.panache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class MongoDbRestDataPanacheIT extends MongoDbRestDataPanacheTest {

    DevServicesContext context;

    @Test
    public void testDevServicesProperties() {
        assertThat(context.devServicesProperties()).isEmpty();
    }
}

package org.acme;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusIntegrationTest
@QuarkusTestResource(DummyResource.class)
public class FruitsEndpointIT extends FruitsEndpointTest {

    @Test
    public void containerNetworkIdSet() {
        Optional<String> optional = DummyResource.CONTAINER_NETWORK_ID.get();
        assertNotNull(optional);
        assertTrue(optional.isPresent());
    }
}

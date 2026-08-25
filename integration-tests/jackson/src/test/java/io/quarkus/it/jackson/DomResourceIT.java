package io.quarkus.it.jackson;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class DomResourceIT extends DomResourceTest {

    // Execute the same tests but in native mode, where the DOM handlers are only present because
    // quarkus.jackson.xml.enabled is set in application.properties.
}

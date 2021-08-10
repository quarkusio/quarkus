package io.quarkus.openapi.generator.it;

import static io.restassured.RestAssured.when;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(WiremockPetStore.class)
public class PetStoreTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class)
                    .addClass(PetResource.class)
                    .addPackage("org.openapitools.client.model")
                    .addPackage("org.openapitools.client.api"));

    @Test
    public void testGetPetById() {
        final String petName = when()
                .get("/petstore/pet/name/1234")
                .then()
                .statusCode(200)
                .extract().asString();
        Assertions.assertEquals("Bidu", petName);
    }
}

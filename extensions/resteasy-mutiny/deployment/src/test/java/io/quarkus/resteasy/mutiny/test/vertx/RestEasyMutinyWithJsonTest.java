package io.quarkus.resteasy.mutiny.test.vertx;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.mutiny.test.MutinyInjector;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RestEasyMutinyWithJsonTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ResourceProducingJsonObject.class, MutinyInjector.class));

    @Test
    public void testUni() {
        String resp = RestAssured.get("/vertx/neo/uni").asString();
        JsonObject json = new JsonObject(resp);
        Assertions.assertEquals("neo", json.getString("Hello"));
    }

    @Test
    public void testMulti() {
        String string = RestAssured.get("/vertx/neo/multi").asString();
        JsonArray json = new JsonArray(string);
        Assertions.assertEquals(1, json.size());
        Assertions.assertEquals("neo", json.getJsonObject(0).getString("Hello"));
    }

}

package io.quarkus.it.mongodb;

import javax.json.bind.Jsonb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class BookResourceWithParameterInjectionTest {

    private static Jsonb jsonb;

    @BeforeAll
    public static void giveMeAMapper() {
        jsonb = Utils.initialiseJsonb();
    }

    @AfterAll
    public static void releaseMapper() throws Exception {
        jsonb.close();
    }

    @Test
    public void testInjectedClient() {
        Utils.callTheEndpoint("/books-with-parameter-injection");
    }
}

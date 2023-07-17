package io.quarkus.it.mongodb;

import jakarta.json.bind.Jsonb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
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

package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ReactiveStreamsOperatorsTestCase {

    @Test
    public void testReactiveStreams() {
        RestAssured.when().get("/reactive/stream-regular").then()
                .body(is("ABC"));
    }

    @Test
    public void testMutiny() {
        RestAssured.when().get("/reactive/stream-mutiny").then()
                .body(is("DEF"));
    }

}

package io.quarkus.it.mockbean;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
class FooResourceTest {

    @InjectMock(returnsDeepMocks = true)
    FooService fooService;

    @Test
    public void testGreet() {
        Mockito.when(fooService.newFoo(anyString()).count(anyInt()).bar(any(Foo.Bar.class)))
                .thenReturn(new Foo.FooBuilder("dummy"));

        String responseStr = given()
                .when().get("/foo/test/100/test2")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertEquals("{\"name\":\"dummy\",\"count\":0,\"bar\":null}", responseStr);
    }
}

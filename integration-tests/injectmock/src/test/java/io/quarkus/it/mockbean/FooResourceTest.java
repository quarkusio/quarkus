package io.quarkus.it.mockbean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.MockitoConfig;

@QuarkusTest
class FooResourceTest {

    @InjectMock
    @MockitoConfig(returnsDeepMocks = true)
    FooService fooService;

    @Test
    public void testGreet() {
        Mockito.when(fooService.newFoo(anyString()).count(anyInt()).bar(any(Foo.Bar.class)))
                .thenReturn(new Foo.FooBuilder("dummy"));

        given()
                .when().get("/foo/test/100/test2")
                .then()
                .statusCode(200)
                .body("name", equalTo("dummy"))
                .body("count", equalTo(0))
                .body("bar", is(nullValue()));
    }
}

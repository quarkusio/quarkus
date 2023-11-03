package io.quarkus.it.mockbean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
class WithTestProfileAndNestedTest {

    @InjectMock
    MessageService messageService;

    @InjectMock
    SuffixService suffixService;

    @BeforeEach
    public void init() {
        doReturn("Hello ")
                .when(messageService).getMessage();

        doReturn("Foo")
                .when(suffixService).getSuffix();
    }

    @Test
    public void testGreet() {
        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("HELLO FOO"));
        Mockito.verify(messageService, Mockito.times(1)).getMessage();
        Mockito.verify(suffixService, Mockito.times(1)).getSuffix();
    }

    @Nested
    @TestProfile(Profile.class)
    class WithTestProfileAndNested {

        @InjectMock
        CapitalizerService capitalizerService;

        @BeforeEach
        public void init() {
            doReturn("BAR").when(suffixService).getSuffix();
            // Do not upper the message and suffix
            when(capitalizerService.capitalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        public void testGreet() {
            given()
                    .when().get("/greeting")
                    .then()
                    .statusCode(200)
                    .body(is("Hello BAR"));
            Mockito.verify(messageService, Mockito.times(1)).getMessage();
            Mockito.verify(suffixService, Mockito.times(1)).getSuffix();
            Mockito.verify(capitalizerService, Mockito.times(1)).capitalize(any());
        }
    }

    public static class Profile implements QuarkusTestProfile {

    }
}

package io.quarkus.it.mockbean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
class GreetingSingletonResourceTest {

    @InjectMock(convertScopes = true)
    MessageServiceSingleton messageService;

    @InjectMock(convertScopes = true)
    SuffixServiceSingleton suffixService;

    @InjectMock(convertScopes = true)
    CapitalizerServiceSingleton capitalizerService;

    @Test
    public void testGreet() {
        Mockito.when(messageService.getMessage()).thenReturn("hi");
        Mockito.when(suffixService.getSuffix()).thenReturn("!");
        mockCapitalizerService();

        given()
                .when().get("/greetingSingleton")
                .then()
                .statusCode(200)
                .body(is("hi!"));
    }

    @Test
    public void testGreetAgain() {
        Mockito.when(messageService.getMessage()).thenReturn("yolo");
        Mockito.when(suffixService.getSuffix()).thenReturn("!!!");
        mockCapitalizerService();

        given()
                .when().get("/greetingSingleton")
                .then()
                .statusCode(200)
                .body(is("yolo!!!"));
    }

    @Test
    public void testMocksNotSet() {
        // when mocks are not configured, they return the Mockito default response
        Assertions.assertNull(messageService.getMessage());
        Assertions.assertNull(suffixService.getSuffix());

        given()
                .when().get("/greetingSingleton")
                .then()
                .statusCode(204);
    }

    private void mockCapitalizerService() {
        Mockito.doAnswer(new Answer() { // don't upper case the string, leave it as it is
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return invocationOnMock.getArgument(0);
            }
        }).when(capitalizerService).capitalize(anyString());
    }
}

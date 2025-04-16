package io.quarkus.it.rest.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.client.main.ClientWithExceptionMapper;
import io.quarkus.it.rest.client.main.MyResponseExceptionMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@Order(100) // used in order to make sure this is run before ShouldNotUseMockTest
public class InjectMockTest {

    @InjectMock
    @RestClient
    ClientWithExceptionMapper mock;

    @BeforeEach
    public void setUp() throws MyResponseExceptionMapper.MyException {
        when(mock.get()).thenReturn("MockAnswer");
    }

    @Test
    void shouldMockClient() throws MyResponseExceptionMapper.MyException {
        assertThat(mock.get()).isEqualTo("MockAnswer");
    }

    @Test
    void shouldMockClientInTheApp() {
        RestAssured.with().post("/call-cdi-client-with-exception-mapper")
                .then()
                .statusCode(503)
                .body(Matchers.equalTo("MockAnswer"));
    }
}

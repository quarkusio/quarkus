package io.quarkus.it.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.client.main.ClientWithExceptionMapper;
import io.quarkus.it.rest.client.main.MyResponseExceptionMapper;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class QuarkusMockTest {

    @RestClient
    ClientWithExceptionMapper client;

    @BeforeEach
    public void setUp() {
        ClientWithExceptionMapper customMock = new ClientWithExceptionMapper() {
            @Override
            public String get() {
                return "MockAnswer";
            }
        };
        QuarkusMock.installMockForType(customMock, ClientWithExceptionMapper.class, RestClient.LITERAL);
    }

    @Test
    void shouldMockClient() throws MyResponseExceptionMapper.MyException {
        assertThat(client.get()).isEqualTo("MockAnswer");
    }

    @Test
    void shouldMockClientInTheApp() {
        RestAssured.with().post("/call-cdi-client-with-exception-mapper")
                .then()
                .statusCode(503)
                .body(Matchers.equalTo("MockAnswer"));
    }
}

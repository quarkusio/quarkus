package io.quarkus.it.hibernate.search.elasticsearch.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HibernateSearchAwsTest {

    @InjectWireMock
    WireMockServer wireMockServer;

    @Test
    public void testSearch() {
        RestAssured.when().put("/test/hibernate-search/init-data").then()
                .statusCode(204);

        RestAssured.when().get("/test/hibernate-search/search").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().put("/test/hibernate-search/purge").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().put("/test/hibernate-search/refresh").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().get("/test/hibernate-search/search-empty").then()
                .statusCode(200);

        RestAssured.when().put("/test/hibernate-search/mass-indexer").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().get("/test/hibernate-search/search").then()
                .statusCode(200)
                .body(is("OK"));

        // All requests must have AWS authentication-related headers
        assertThat(wireMockServer.findAll(RequestPatternBuilder.allRequests()))
                .isNotEmpty() // Just to be sure the code below actually asserts something
                .allSatisfy(request -> {
                    assertHeader(request, "Authorization")
                            .matches("AWS4-HMAC-SHA256 Credential=[a-zA-Z0-9]+/[0-9]+/us-east-1/es/aws4_request,"
                                    + " SignedHeaders=host;x-amz-date, Signature=[a-f0-9]+");
                    assertHeader(request, "X-Amz-Date")
                            .matches("[0-9]{8}T[0-9]{6}Z");
                });
    }

    private AbstractStringAssert<?> assertHeader(LoggedRequest request, String headerName) {
        return assertThat(request.getHeader(headerName))
                .as("'" + headerName + "' header");
    }

}

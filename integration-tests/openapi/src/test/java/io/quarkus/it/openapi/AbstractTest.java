package io.quarkus.it.openapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.RestAssured;

public abstract class AbstractTest {
    private static final Logger LOG = Logger.getLogger(AbstractTest.class);
    private static final String OPEN_API_PATH = "/q/openapi";

    protected static final String TEXT_PLAIN = "text/plain";
    protected static final String APPLICATION_JSON = "application/json";

    @Inject
    protected ObjectMapper om;

    protected void testServiceRequest(String path, String expectedContentType, String expectedValue) {
        // Service
        RestAssured
                .with().body(expectedValue)
                .and()
                .with().contentType(expectedContentType)
                .when()
                .post(path)
                .then()
                .header("Content-Type", Matchers.startsWith(expectedContentType))
                .and()
                .body(Matchers.equalTo(expectedValue));
    }

    protected void testServiceResponse(String path, String expectedResponseType, String expectedValue) {
        // Service
        RestAssured
                .when()
                .get(path)
                .then()
                .header("Content-Type", Matchers.startsWith(expectedResponseType))
                .and()
                .body(Matchers.equalTo(expectedValue));
    }

    protected void testOpenAPIRequest(String path, String expectedRequestType) {
        // OpenAPI
        RestAssured.given().queryParam("format", "JSON")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths.'" + path + "'.post.requestBody.content.'" + expectedRequestType + "'",
                        Matchers.notNullValue());
    }

    protected void testOpenAPIResponse(String path, String expectedResponseType) {
        // OpenAPI
        RestAssured.given().queryParam("format", "JSON")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths.'" + path + "'.get.responses.'200'.content.'" + expectedResponseType + "'",
                        Matchers.notNullValue());
    }

    protected String createExpected(String message) {
        try {
            Greeting g = new Greeting(0, message);
            return om.writeValueAsString(g);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String createExpectedList(String message) {
        try {
            List<Greeting> l = new ArrayList<>();
            l.add(new Greeting(0, message));
            return om.writeValueAsString(l);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String createExpectedMap(String message) {
        try {
            Map<String, Greeting> m = new HashMap<>();
            m.put(message, new Greeting(0, message));
            return om.writeValueAsString(m);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }
}

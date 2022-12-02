package io.quarkus.smallrye.openapi.test.hotreload;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class DisplayOpenAPiEndpointInNotFoundExceptionPageTest {
    private static final String OPEN_API_PATH = "/openapi-path";
    private static final String SWAGGER_UI_PATH = "/swagger-path";

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyResource.class)
                    .addAsResource(new StringAsset(
                            "quarkus.smallrye-openapi.path=" + OPEN_API_PATH + "\nquarkus.swagger-ui.path=" + SWAGGER_UI_PATH),
                            "application.properties"));

    @Test
    public void shouldDisplayOpenApiAndSwaggerUiEndpointsInNotFoundPage() {
        RestAssured
                .given()
                .accept(ContentType.HTML)
                .when()
                .get("/open")
                .then()
                .statusCode(404)
                .body(containsString(OPEN_API_PATH))
                .body(containsString(SWAGGER_UI_PATH));
    }
}

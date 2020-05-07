package io.quarkus.swaggerui.deployment;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-custom-config.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties"));

    @Test
    public void shouldUseCustomConfig() {
        final Matcher<String> stringContainsInOrder = Matchers.stringContainsInOrder(
                "const ui = SwaggerUIBundle(",
                "deepLinking : true",
                "defaultModelExpandDepth : -1",
                "defaultModelRendering : \"model\"",
                "defaultModelsExpandDepth : -1",
                "displayOperationId : true",
                "displayRequestDuration : true",
                "docExpansion : \"none\"",
                "dom_id : \"#swagger-ui\"",
                "filter : true",
                "layout : \"StandaloneLayout\"",
                "maxDisplayedTags : 5",
                "oauth2RedirectUrl : \"/custom/oauth2-redirect.html\"",
                "operationsSorter : \"method\"",
                "showCommonExtensions : true",
                "showExtensions : true",
                "supportedSubmitMethods : [ \"options\", \"get\", \"head\", \"post\", \"put\", \"delete\", \"trace\", \"connect\", \"patch\", \"other\" ]",
                "tagsSorter : \"alpha\"",
                "url : \"/openapi\"",
                "urls : [ {\n" +
                        "    url : \"/openapi\",\n" +
                        "    name : \"test\"\n" +
                        "  }, {\n" +
                        "    url : \"/openapi\",\n" +
                        "    name : \"lahzouz\"\n" +
                        "  } ],",
                "urls.primaryName : \"/openapi?group=test\"",
                "validatorUrl : \"https://validator.swagger.io/validator\"");

        RestAssured.when().get("/custom").then().statusCode(200).body(stringContainsInOrder);
        RestAssured.when().get("/custom/index.html").then().statusCode(200).body(stringContainsInOrder);
    }

    @Test
    public void shouldUseOauthCustomConfig() {
        RestAssured.when().get("/custom/index.html").then().statusCode(200)
                .body(Matchers.stringContainsInOrder(
                        "appName : \"quarkus\"",
                        "clientId : \"your-client-id\"",
                        "clientSecret : \"your-client-secret-if-required\"",
                        "realm : \"your-realms\"",
                        "scopeSeparator : \"########\"",
                        "useBasicAuthenticationWithAccessCodeGrant : true",
                        "usePkceWithAuthorizationCodeGrant : true"));
        RestAssured.when().get("/custom/oauth2-redirect.html").then().statusCode(200);
    }
}

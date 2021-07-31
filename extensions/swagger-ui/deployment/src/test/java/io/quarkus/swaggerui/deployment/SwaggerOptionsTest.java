package io.quarkus.swaggerui.deployment;

import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SwaggerOptionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(getPropertyAsString()), "application.properties"));

    @Test
    public void customOptions() {
        RestAssured.when().get("/q/swagger-ui").then().statusCode(200)
                .body(
                        containsString("Testing title"),
                        containsString("/openapi"),
                        containsString("https://petstore.swagger.io/v2/swagger.json"),
                        containsString("theme-newspaper.css"),
                        containsString("docExpansion: 'full'"),
                        containsString("var oar = \"/somesecure/page/oauth.html\";"),
                        containsString("validatorUrl: 'localhost'"),
                        containsString("displayRequestDuration: true"),
                        containsString("supportedSubmitMethods: ['get', 'post']"),
                        containsString("plugins: [Plugin1, Plugin2]"));

    }

    private static String getPropertyAsString() {
        try {
            Properties p = new Properties();
            p.putAll(PROPERTIES);
            StringWriter writer = new StringWriter();
            p.store(writer, "Test Properties");
            return writer.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected static final Properties PROPERTIES = new Properties();
    static {
        PROPERTIES.put("quarkus.swagger-ui.title", "Testing title");
        PROPERTIES.put("quarkus.swagger-ui.urls.default", "/openapi");
        PROPERTIES.put("quarkus.swagger-ui.urls.petstore", "https://petstore.swagger.io/v2/swagger.json");
        PROPERTIES.put("quarkus.swagger-ui.doc-expansion", "full");
        PROPERTIES.put("quarkus.swagger-ui.theme", "newspaper");
        PROPERTIES.put("quarkus.swagger-ui.oauth2-redirect-url", "/somesecure/page/oauth.html");
        PROPERTIES.put("quarkus.swagger-ui.validator-url", "localhost");
        PROPERTIES.put("quarkus.swagger-ui.display-request-duration", "true");
        PROPERTIES.put("quarkus.swagger-ui.supported-submit-methods", "get,post");
        PROPERTIES.put("quarkus.swagger-ui.plugins", "Plugin1,Plugin2");
    }
}

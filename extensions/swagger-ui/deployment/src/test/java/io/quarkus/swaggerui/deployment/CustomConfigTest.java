package io.quarkus.swaggerui.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class CustomConfigTest {

    private static final Pattern URL_PATTERN = Pattern.compile("\\burl: '([^']+)'");

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.swagger-ui.path=/custom"), "application.properties"));

    @Test
    public void shouldUseCustomConfig() {
        String indexHtml = RestAssured.when().get("/custom/index.html").then().statusCode(200).extract().asString();
        Matcher matcher = URL_PATTERN.matcher(indexHtml);
        assertTrue(matcher.find());
        assertEquals("/prefix/q/openapi", URI.create("http://h/prefix/custom/").resolve(matcher.group(1)).getPath());
    }
}

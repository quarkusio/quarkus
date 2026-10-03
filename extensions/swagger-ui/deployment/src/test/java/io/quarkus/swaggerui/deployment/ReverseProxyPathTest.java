package io.quarkus.swaggerui.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ReverseProxyPathTest {

    private static final Pattern URL_PATTERN = Pattern.compile("\\burl: '([^']+)'");
    private static final Pattern SERVER_BASE_PATTERN = Pattern.compile("var base = new URL\\(\"([^\"]+)\"");

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.http.non-application-root-path", "/");

    @Test
    public void shouldUseRelativeOpenApiUrl() {
        String indexHtml = RestAssured.when().get("/swagger-ui/index.html").then().statusCode(200).extract().asString();
        assertResolvedPath(indexHtml, URL_PATTERN, "http://h/prefix/swagger-ui/", "/prefix/openapi");
        assertResolvedPath(indexHtml, SERVER_BASE_PATTERN, "http://h/prefix/swagger-ui/", "/prefix/");
    }

    private static void assertResolvedPath(String indexHtml, Pattern pattern, String pageUrl, String expectedPath) {
        Matcher matcher = pattern.matcher(indexHtml);
        assertTrue(matcher.find());
        assertEquals(expectedPath, URI.create(pageUrl).resolve(matcher.group(1)).getPath());
    }
}

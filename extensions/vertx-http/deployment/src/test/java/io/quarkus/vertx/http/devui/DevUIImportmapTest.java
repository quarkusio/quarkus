package io.quarkus.vertx.http.devui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class DevUIImportmapTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    @Test
    public void testImportMap() {
        Response response = RestAssured.given()
                .when()
                .get("q/dev-ui/configuration-form-editor").then()
                .statusCode(200)
                .extract()
                .response();

        String htmlContent = response.asString();
        Pattern pattern = Pattern.compile("<script type=\"importmap\">(.*?)</script>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(htmlContent);

        assertThat("Script tag not found", matcher.find(), is(true));
        String importmapjson = matcher.group(1);
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Map<String, String>> importsMap = mapper.readValue(importmapjson, Map.class);
            Map<String, String> importMap = importsMap.get("imports");
            assertThat("Normal mapping (@qomponent/qui-badge) not present in the importmap",
                    importMap.containsKey("@qomponent/qui-badge"), is(true));
            assertThat("Relocated mapping (qui-badge) not present in the importmap",
                    importMap.containsKey("qui-badge"), is(true));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }

    }

}

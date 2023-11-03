package io.quarkus.virtual.mail;

import static org.hamcrest.Matchers.is;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@QuarkusTest
@QuarkusTestResource(MailHogResource.class)
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest {

    @Test
    void test() {
        RestAssured.get().then()
                .assertThat().statusCode(200)
                .body(is("OK"));

        var url = ConfigProvider.getConfig().getValue("mailhog.url", String.class);
        var content = RestAssured.get(url).thenReturn().asPrettyString();

        JsonObject json = new JsonObject(content);
        String body = findMessageBody("test simple", json.getJsonArray("items"));
        Assertions.assertThat(body).isEqualTo("This email is sent from a virtual thread");
    }

    @Test
    void testWithTemplate() {
        RestAssured.get("/template").then()
                .assertThat().statusCode(200)
                .body(is("OK"));

        var url = ConfigProvider.getConfig().getValue("mailhog.url", String.class);
        var content = RestAssured.get(url).thenReturn().asPrettyString();

        JsonObject json = new JsonObject(content);
        String body = findMessageBody("test template", json.getJsonArray("items"));
        Assertions.assertThat(body).isEqualTo("Hello virtual threads!");
    }

    private String findMessageBody(String subject, JsonArray items) {
        for (Object item : items) {
            var json = (JsonObject) item;
            var content = json.getJsonObject("Content");
            if (content != null) {
                var subjects = content.getJsonObject("Headers").getJsonArray("Subject");
                if (subjects != null && subject.equals(subjects.getString(0))) {
                    return content.getString("Body");
                }
            }
        }
        return null;
    }

}

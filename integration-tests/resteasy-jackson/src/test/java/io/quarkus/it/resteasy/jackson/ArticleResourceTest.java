package io.quarkus.it.resteasy.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class ArticleResourceTest {

    private static final Article CREATE_ARTICLE_REQUEST = new Article();
    static {
        CREATE_ARTICLE_REQUEST.setTitle("How to train your dragon");
        CREATE_ARTICLE_REQUEST.setDescription("Ever wonder how?");
        CREATE_ARTICLE_REQUEST.setBody("Very carefully");
        CREATE_ARTICLE_REQUEST.setTagList(Arrays.asList("dragons", "training"));
    }

    @Test
    public void testCreateArticle() {
        given()
                .when()
                .body(CREATE_ARTICLE_REQUEST)
                .contentType(ContentType.JSON)
                .post("/article")
                .then()
                .statusCode(200)
                .body("art.title", equalTo("How to train your dragon"))
                .body("art.description", equalTo("Ever wonder how?"))
                .body("art.body", equalTo("Very carefully"));
    }
}

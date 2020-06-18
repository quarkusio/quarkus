package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PostResourceTest {

    @Test
    void testAll() {
        List<Post> posts = when().get("/post/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Post.class);

        assertThat(posts)
                .hasSize(3)
                .filteredOn(p -> p.getTitle().contains("first"))
                .hasOnlyOneElementSatisfying(p -> {
                    assertThat(p.getComments()).hasSize(2);
                });
    }

    @Test
    void testByBypassTrue() {
        when().get("/post/bypass/true").then()
                .statusCode(204);
    }

    @Test
    void testByPostedAtBefore() {
        when().get("/post/postedBeforeNow").then()
                .statusCode(200)
                .body("size()", is(3));
    }

    @Test
    void testByByOrganization() {
        when().get("/post/organization/RH").then()
                .statusCode(200)
                .body("size()", is(3));

        when().get("/post/organization/RHT").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void testPostCommentByPostId() {
        when().get("/post/postComment/postId/1").then()
                .statusCode(200)
                .body("size()", is(2));

        when().get("/post/postComment/postId/10").then()
                .statusCode(200)
                .body("size()", is(0));
    }
}

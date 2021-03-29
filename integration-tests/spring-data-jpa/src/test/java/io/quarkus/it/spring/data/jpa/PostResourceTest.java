package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostResourceTest {

    @Test
    @Order(1)
    void testAll() {
        List<Post> posts = when().get("/post/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Post.class);

        assertThat(posts)
                .hasSize(3)
                .filteredOn(p -> p.getTitle().contains("first"))
                .singleElement().satisfies(p -> {
                    assertThat(p.getComments()).hasSize(2);
                });
    }

    @Test
    @Order(2)
    void testByBypassTrue() {
        when().get("/post/bypass/true").then()
                .statusCode(204);
    }

    @Test
    @Order(3)
    void testByPostedAtBefore() {
        when().get("/post/postedBeforeNow").then()
                .statusCode(200)
                .body("size()", is(3));
    }

    @Test
    @Order(4)
    void testByByOrganization() {
        when().get("/post/organization/RH").then()
                .statusCode(200)
                .body("size()", is(3));

        when().get("/post/organization/RHT").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @Order(5)
    void testPostCommentByPostId() {
        when().get("/post/postComment/postId/1").then()
                .statusCode(200)
                .body("size()", is(2));

        when().get("/post/postComment/postId/10").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @Order(6)
    void testAllPostComment() {
        when().get("/post/postComment/all").then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    @Order(7)
    void testFindPostByTitleContainingText() {
        Post post = when().get("/post/mandatory/1").then()
                .statusCode(200)
                .extract().body().as(Post.class);

        assertThat(post).isNotNull();
        assertThat(post.getId()).isEqualTo(1L);
    }

    @Test
    @Order(8)
    public void testDeleteAllPosts() {
        //We create a post with metadata
        when().post("/post/postId/1/key/key1/value/value1").then()
                .statusCode(200);

        when().post("/post/postId/2/key/key2/value/value2").then()
                .statusCode(200);

        when().get("/post/delete/all").then()
                .statusCode(204);
        List<Post> posts = when().get("/post/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Post.class);
        assertThat(posts).isEmpty();

        List<PostComment> comments = when().get("/post/postComment/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", PostComment.class);
        assertThat(comments).isEmpty();
    }

    @Test
    @Order(9)
    public void testDeletePostByOrganization() {
        Post postHow = when().get("/post/new/title/howTo/organization/stackOverflow").then()
                .statusCode(200)
                .extract().body().as(Post.class);

        List<Post> posts = when().get("/post/organization/stackOverflow").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Post.class);

        assertThat(posts).isNotEmpty();
        assertThat(posts.size()).isEqualTo(1);
        assertThat(posts.get(0).getComments()).isNotEmpty();

        when().get("/post/delete/byOrg/stackOverflow").then()
                .statusCode(204);

        posts = when().get("/post/organization/stackOverflow").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Post.class);

        assertThat(posts).isEmpty();

    }

    @Test
    @Order(10)
    void testDoNothing() {
        when().get("/post/doNothing").then()
                .statusCode(204);

    }

}

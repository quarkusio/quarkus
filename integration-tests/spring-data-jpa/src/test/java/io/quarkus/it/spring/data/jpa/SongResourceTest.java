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
public class SongResourceTest {

    @Test
    @Order(1)
    void testAll() {
        when().get("/song/allPages").then()
                .statusCode(200)
                .body(is("false - false / 6"));
    }

    @Test
    @Order(2)
    void testFindFirstPageWithTwoElements() {
        when().get("/song/page/0/2").then()
                .statusCode(200)
                .body(is("false - true / 2"));
    }

    @Test
    @Order(3)
    public void testDeleteByIdWithCascade() {
        Person fiona = when().get("/person/new/fiona").then()
                .statusCode(200)
                .extract().body().as(Person.class);

        Song song = when().get("/song/new/one/author/metallica").then()
                .statusCode(200)
                .extract().body().as(Song.class);

        // Fiona likes anySong
        when().get("/person/" + fiona.getId() + "/song/" + song.getId()).then()
                .statusCode(200)
                .extract().body().as(Person.class);

        List<Song> songsLikedByFiona = when().get("/person/" + fiona.getId() + "/songs").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Song.class);

        assertThat(songsLikedByFiona).isNotEmpty();

        when().get("/song/delete/id/" + song.getId()).then()
                .statusCode(204);

        songsLikedByFiona = when().get("/person/" + fiona.getId() + "/songs").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Song.class);

        assertThat(songsLikedByFiona).isEmpty();

        song = when().get("/song/id/" + song.getId()).then()
                .statusCode(200)
                .extract().body().as(Song.class);

        assertThat(song).isNull();

        when().get("/person/delete/" + fiona.getId()).then()
                .statusCode(204);

    }

    @Test
    @Order(4)
    public void testDeleteAllWithCascade() {
        List<Song> all = when().get("/song/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Song.class);

        assertThat(all).isNotEmpty();

        when().get("/song/delete/all/").then()
                .statusCode(204);

        all = when().get("/song/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Song.class);

        assertThat(all).isEmpty();
    }

    @Test
    @Order(5)
    public void testDefaultMethod() {
        when().get("/song/doNothing").then()
                .statusCode(204);
    }

}

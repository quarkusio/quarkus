package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class MovieResourceTest {

    private static final Set<String> NOT_ADDED_OR_REMOVED = new HashSet<>(
            Arrays.asList("Godzilla: King of the Monsters", "Avengers: Endgame", "Interstellar", "Aladdin"));

    @Test
    void testAll() {
        List<Movie> movies = when().get("/movie/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Movie.class);

        // make sure /all contains the elements we know that are never removed
        assertThat(movies).extracting("title").filteredOn(NOT_ADDED_OR_REMOVED::contains)
                .containsExactlyInAnyOrder(new ArrayList<>(NOT_ADDED_OR_REMOVED).toArray(new String[0]));
    }

    @Test
    void testFindFirstByOrderByDurationDesc() {
        when().get("/movie/first/orderByDuration").then()
                .statusCode(200)
                .body(containsString("Avengers: Endgame"));
    }

    @Test
    void testFindByRating() {
        when().get("/movie/rating/Dummy").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/movie/rating/PG-13").then()
                .statusCode(200)
                .body("size()", is(3))
                .body(containsString("Godzilla: King of the Monsters"))
                .body(containsString("Avengers: Endgame"))
                .body(containsString("Interstellar"));
    }

    @Test
    void testFindByTitle() {
        when().get("/movie/title/Dummy").then()
                .statusCode(204);

        when().get("/movie/title/Aladdin").then()
                .statusCode(200)
                .body(containsString("Aladdin"));
    }

    @Test
    void testWithRatingAndDurationLargerThan() {
        when().get("/movie/rating/Dummy/durationLargerThan/0").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/movie/rating/PG-13/durationLargerThan/180").then()
                .statusCode(200)
                .body("size()", is(1))
                .body(containsString("Avengers: Endgame"));

        when().get("/movie/rating/PG-13/durationLargerThan/160").then()
                .statusCode(200)
                .body("size()", is(2))
                .body(containsString("Avengers: Endgame"))
                .body(containsString("Interstellar"));
    }

    @Test
    void testFetchOnlySomeFieldsWithTitleLike() {
        when().get("/movie/title/like/Dummy").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/movie/title/like/la").then()
                .statusCode(200)
                .body("size()", is(3))
                .body(containsString("Godzilla: King of the Monsters"))
                .body(containsString("Interstellar"))
                .body(containsString("Aladdin"));
    }

    @Test
    void testDeleteByRating() {
        when().get("/movie/all").then()
                .statusCode(200)
                .body(containsString("Die Hard"))
                .body(containsString("The Departed"));

        when().get("/movie/delete/rating/R").then()
                .statusCode(204);

        when().get("/movie/all").then()
                .statusCode(200)
                .body(not(containsString("Die Hard")))
                .body(not(containsString("The Departed")))
                .body(containsString("Interstellar"));
    }

    @Test
    void testDeleteByTitleLike() {
        when().get("/movie/all").then()
                .statusCode(200)
                .body(containsString("Dunkirk"));

        when().get("/movie/delete/title/nkir").then()
                .statusCode(200)
                .body(is("1"));

        when().get("/movie/all").then()
                .statusCode(200)
                .body(not(containsString("Dunkirk")))
                .body(containsString("Interstellar"));
    }

    @Test
    void testChangeRatingToNewName() {
        when().get("/movie/change/rating/PG/NEWRATING").then()
                .statusCode(200)
                .body(is("1"));

        when().get("/movie/all").then()
                .statusCode(200)
                .body(containsString("NEWRATING"));
    }

    @Test
    void testSetRatingToNullForTitle() {
        when().get("/movie/rating/G").then()
                .statusCode(200)
                .body("size()", is(1));

        when().get("/movie/nullify/rating/forTitle/Toy Story 4").then()
                .statusCode(204);

        when().get("/movie/rating/G").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void testOrderByTitleLength() {
        when().get("/movie/title/titleLengthOrder/page/1/0").then()
                .statusCode(200)
                .body(is("true / 1"));

        when().get("/movie/title/titleLengthOrder/page/1/1").then()
                .statusCode(200)
                .body(is("true / 1"));

        when().get("/movie/title/titleLengthOrder/page/10/0").then()
                .statusCode(200)
                .body(containsString("false /"));
    }

    @Test
    void testCustomFind() {
        when().get("/movie/customFind/page/1/0").then()
                .statusCode(200)
                .body(is("true / 1"));

        when().get("/movie/customFind/page/1/1").then()
                .statusCode(200)
                .body(is("true / 1"));

        when().get("/movie/customFind/page/10/0").then()
                .statusCode(200)
                .body(containsString("false /"));

        List<Movie> movies = when().get("/movie/customFind/all").then()
                .statusCode(200)
                .extract().body().as(new TypeRef<List<Movie>>() {
                });
        List<Long> ids = movies.stream().map(MovieSuperclass::getId).collect(Collectors.toList());
        assertThat(ids).isNotEmpty().isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void testCountByRating() {
        when().get("/movie/count/rating").then()
                .statusCode(200)
                .body(containsString("rating"));
    }

    @Test
    void testFindAllRatings() {
        when().get("/movie/ratings").then()
                .statusCode(200)
                .body(containsString("PG"))
                .body(containsString("PG-13"));
    }

    @Test
    void testFindRatingByTitle() {
        when().get("/movie/rating/forTitle/Interstellar").then()
                .statusCode(200)
                .body(containsString("Interstellar"))
                .body(containsString("PG-13"))
                .body(not(containsString("duration")));
    }

    @Test
    void testFindOptionalRatingByTitle() {
        when().get("/movie/rating/opt/forTitle/Aladdin").then()
                .statusCode(200)
                .body(containsString("Aladdin"))
                .body(not(containsString("duration")));
    }

    @Test
    void testNewMovie() {
        long id = 999L;
        String title = "tenet";
        Movie movie = when().get(String.format("/movie/new/%d/%s", id, title)).then()
                .statusCode(200)
                .extract().body().as(Movie.class);

        assertThat(movie.getId()).isEqualTo(id);
        assertThat(movie.getTitle()).isEqualTo(title);
        assertThat(movie.getVersion()).isNotNull();

        when().get("/movie/title/" + title).then()
                .statusCode(200)
                .body(containsString(title));

        when().get("/movie/delete/title/" + title).then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    void getTitle() {
        when().get("/movie/titles/rating/PG-13").then()
                .statusCode(200)
                .body(containsString("Godzilla"));
    }
}

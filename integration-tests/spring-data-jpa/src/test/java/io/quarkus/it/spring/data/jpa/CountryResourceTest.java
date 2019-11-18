package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CountryResourceTest {

    private static final Set<String> NOT_ADDED_OR_REMOVED = new HashSet<>(
            Arrays.asList("Greece", "France", "Czechia"));

    @Test
    void testAll() {
        List<Country> countries = when().get("/country/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Country.class);

        // make sure /all contains the elements we know that are never removed
        assertThat(countries).extracting("name").filteredOn(NOT_ADDED_OR_REMOVED::contains)
                .containsExactlyInAnyOrder(new ArrayList<>(NOT_ADDED_OR_REMOVED).toArray(new String[0]));
    }

    @Test
    void testPage() {
        when().get("/country/page/1/0").then()
                .statusCode(200)
                .body(is("false - true / 1"));

        when().get("/country/page/1/1").then()
                .statusCode(200)
                .body(is("true - true / 1"));

        when().get("/country/page/10/0").then()
                .statusCode(200)
                .body(startsWith("false - false / "));

        when().get("/country/page/10/1").then()
                .statusCode(200)
                .body(is("true - false / 0"));
    }

    @Test
    void testPageSorted() {
        String response = when().get("/country/page-sorted/2/0").then()
                .statusCode(200)
                .extract().response().asString();
        assertThat(Arrays.stream(response.split(",")).map(Long::parseLong).collect(Collectors.toList()))
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void testGetOne() {
        when().get("/country/getOne/1").then()
                .statusCode(200)
                .body(containsString("Greece"));

        when().get("/country/getOne/100").then()
                .statusCode(500);
    }

    @Test
    void testNewAndEditIso() {
        when().get("/country/all").then()
                .statusCode(200)
                .body(not(containsString("Germany")));

        when().get("/country/new/Germany/GER").then()
                .statusCode(200)
                .body(containsString("Germany"));

        when().get("/country/all").then()
                .statusCode(200)
                .body(containsString("Germany"))
                .body(containsString("GER"));

        when().get("/country/editIso3/4/DEU").then()
                .statusCode(200)
                .body(containsString("Germany"));

        when().get("/country/all").then()
                .statusCode(200)
                .body(containsString("Germany"))
                .body(containsString("DEU"))
                .body(not(containsString("GER")));

        when().get("/country/editIso3/100/ZZZ").then()
                .statusCode(500);
    }
}

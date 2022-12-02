package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersonResourceTest {

    private static final Set<String> NOT_ADDED_OR_REMOVED = new HashSet<>(
            Arrays.asList("Bob", "Florence", "DeMar", null));

    @Test
    @Order(1)
    void testFindAll() {
        when().get("/person/all").then()
                .statusCode(200)
                .body(containsString("DeMar"))
                .body(containsString("Florence"))
                .body(containsString("Bob"));
    }

    @Test
    @Order(2)
    void testCount() {
        String count = when().get("/person/count").then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(Integer.valueOf(count)).isGreaterThanOrEqualTo(NOT_ADDED_OR_REMOVED.size());
    }

    @Test
    @Order(3)
    void testFindById() {
        when().get("/person/id/1").then()
                .statusCode(200)
                .body(containsString("Bob"));

        when().get("/person/id/1000").then()
                .statusCode(200)
                .body(is("null"));
    }

    @Test
    @Order(4)
    void testExistsById() {
        when().get("/person/exists/id/1").then()
                .statusCode(200)
                .body(is("true"));

        when().get("/person/exists/id/1000").then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    @Order(5)
    void testFindByName() {
        when().get("/person/name/Dummy").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/person/name/DeMar").then()
                .statusCode(200)
                .body("size()", is(3));
    }

    @Test
    @Order(6)
    void testFindByNamePageSorted() {
        String response = when().get("/person/name-pageable/DeMar").then()
                .statusCode(200)
                .extract().response().asString();
        assertThat(Arrays.stream(response.split(",")).map(Long::parseLong).collect(Collectors.toList()))
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @Order(7)
    void testFindBySortedByJoinedDesc() {
        List<Person> people = when().get("/person/name/DeMar/order/joined").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(people).extracting("name", "age")
                .containsExactly(tuple("DeMar", 20), tuple("DeMar", 28), tuple("DeMar", 55));
    }

    @Test
    @Order(8)
    void testPageHandlingFindByNameSortedByJoined() {
        when().get("/person/name/joinedOrder/Dummy/page/10/0").then()
                .statusCode(200)
                .body(is("false - false / 0"));

        when().get("/person/name/joinedOrder/DeMar/page/2/0").then()
                .statusCode(200)
                .body(is("false - true / 2"));

        when().get("/person/name/joinedOrder/DeMar/page/2/1").then()
                .statusCode(200)
                .body(is("true - false / 1"));
    }

    @Test
    @Order(9)
    void testFindBySortedByAgeDesc() {
        List<Person> people = when().get("/person/name/ageOrder/DeMar/page/5/0").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(people).extracting("name", "age")
                .containsExactly(tuple("DeMar", 55), tuple("DeMar", 28), tuple("DeMar", 20));

        when().get("/person/name/ageOrder/DeMar/page/5/1").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/person/name/ageOrder/DeMar/page/2/0").then()
                .statusCode(200)
                .body("size()", is(2));
        when().get("/person/name/ageOrder/DeMar/page/2/1").then()
                .statusCode(200)
                .body("size()", is(1));
        when().get("/person/name/ageOrder/DeMar/page/2/2").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/person/name/ageOrder/Dummy/page/2/0").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @Order(10)
    void testFindByNameOrderByAge() {
        when().get("/person/name/ageOrder/Dummy").then()
                .statusCode(200)
                .body("size()", is(0));

        List<Person> people = when().get("/person/name/ageOrder/DeMar").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(people).extracting("name", "age")
                .containsExactly(tuple("DeMar", 20), tuple("DeMar", 28), tuple("DeMar", 55));

    }

    @Test
    @Order(11)
    void testFindByAgeBetweenAndNameIsNotNull() {
        List<Person> people = when().get("/person/age/between/20/41").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(people)
                .filteredOn(p -> NOT_ADDED_OR_REMOVED.contains(p.getName()))
                .extracting("name", "age")
                .containsOnly(tuple("Florence", 41), tuple("DeMar", 28), tuple("DeMar", 20));
    }

    @Test
    @Order(12)
    void testFindByAgeGreaterThanEqualOrderByAgeAsc() {
        when().get("/person/age/greaterEqual/55").then()
                .statusCode(200)
                .body("size()", is(1))
                .body(containsString("DeMar"));
    }

    @Test
    @Order(13)
    void testJoinedAfter() {
        when().get("/person/joined/afterDaysAgo/0").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/person/joined/afterDaysAgo/20000").then()
                .statusCode(200)
                .body(containsString("DeMar"))
                .body(containsString("Florence"))
                .body(containsString("Bob"))
                .body(containsString("null"));
    }

    @Test
    @Order(14)
    void testActiveTrueOrderByAgeDesc() {
        List<Person> people = when().get("/person/active").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(people)
                .filteredOn(p -> NOT_ADDED_OR_REMOVED.contains(p.getName()))
                .extracting("name", "age")
                .containsExactly(tuple("Bob", 43), tuple("Florence", 41), tuple(null, 22), tuple("DeMar", 20));
    }

    @Test
    @Order(15)
    void testCountByActiveNot() {
        // find all non active people
        String count = when().get("/person/count/activeNot/true").then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(Integer.valueOf(count)).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(16)
    void testFindTop3ByActive() {
        List<Person> people = when().get("/person/active/top3").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(people)
                .filteredOn(p -> NOT_ADDED_OR_REMOVED.contains(p.getName()))
                .extracting("name", "age")
                .containsExactly(tuple("Bob", 43), tuple("Florence", 41), tuple(null, 22));
    }

    @Test
    @Order(17)
    void testFindPeopleBySomeAddressZipCode() {
        when().get("/person/addressZipCode/00000").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/person/addressZipCode/123456").then()
                .statusCode(200)
                .body("size()", is(2))
                .body(containsString("Bob"))
                .body(containsString("DeMar"));
    }

    @Test
    @Order(18)
    void testFindBySomeAddressId() {
        when().get("/person/addressId/00000").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/person/addressId/2").then()
                .statusCode(200)
                .body("size()", is(1))
                .body(containsString("Florence"));
    }

    @Test
    @Order(19)
    void testFindBySomeAddressStreetNumber() {
        when().get("/person/addressStreetNumber/whatever").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/person/addressStreetNumber/10000").then()
                .statusCode(200)
                .body("size()", is(2))
                .body(containsString("Bob"), containsString("DeMar"));
    }

    @Test
    @Order(20)
    void testNewPerson() {
        Person person = when().get("/person/new/user").then()
                .statusCode(200)
                .extract().body().as(Person.class);

        assertThat(person.getId()).isGreaterThanOrEqualTo(100);
        assertThat(person.getName()).isEqualTo("USER");

        when().get("/person/exists/id/" + person.getId()).then()
                .statusCode(200)
                .body(is("true"));

        when().get("/person/delete/" + person.getId()).then()
                .statusCode(204);

        when().get("/person/exists/id/" + person.getId()).then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    @Order(21)
    void testNewPeople() {
        List<Person> people = when().get("/person/new/newUser/times/10").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(people).allSatisfy(p -> {
            assertThat(p.getName()).isEqualTo("newUser");
            assertThat(p.getId()).isGreaterThanOrEqualTo(100);
        });

        Set<String> ids = getIds(people);

        List<Person> peopleFromExists = when().get("/person/ids/" + String.join(",", ids)).then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(peopleFromExists.size()).isEqualTo(people.size());
        assertThat(getIds(peopleFromExists)).containsExactlyInAnyOrder(new HashSet<>(ids).toArray(new String[0]));

        when().get("/person/delete/name/first/newUser").then()
                .statusCode(204);

        List<Person> peopleFromExistsAfterFirstDelete = when().get("/person/ids/" + String.join(",", ids)).then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(peopleFromExistsAfterFirstDelete.size()).isEqualTo(peopleFromExists.size() - 1);

        when().get("/person/delete/name/all/newUser").then()
                .statusCode(204);

        when().get("/person/name/newUser").then()
                .statusCode(200)
                .body("size()", is(0));

    }

    @Test
    @Order(22)
    public void testDeletePersonWithChildItems() {
        // Given a person who likes some songs
        Person alice = when().get("/person/new/alice").then()
                .statusCode(200)
                .extract().body().as(Person.class);

        populateSongsDb();

        makePersonLikeSong(alice.getId());

        Long likedSongId = when().get("/person/" + alice.getId() + "/songs").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Song.class).get(0).getId();

        //when person is removed
        when().get("/person/delete/age/" + alice.getAge()).then()
                .statusCode(204);

        List<Song> songsLikedByAlice = when().get("/person/" + alice.getId() + "/songs").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Song.class);

        //Then the liked songs table is also removed for that person
        assertThat(songsLikedByAlice).hasSize(0);

        Song song = when().get("/song/id/" + likedSongId).then()
                .statusCode(200)
                .extract().body().as(Song.class);

        //the song is not removed
        assertThat(song).isNotNull();

        deleteCreatedSongs();

    }

    @Test
    @Order(23)
    public void testDeleteAllWithCascade() {
        List<Person> all = when().get("/person/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(all).isNotEmpty();

        when().get("person/delete/all").then()
                .statusCode(204);

        List<Person> nobody = when().get("/person/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Person.class);
        assertThat(nobody).isEmpty();
    }

    private void makePersonLikeSong(Long personId) {
        List<Song> allSongs = when().get("/song/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Song.class);

        assertThat(allSongs).isNotEmpty();

        //get a random song id to add to Alice
        Long anySong = allSongs.stream().findFirst().map(Song::getId).get();

        // Alice likes anySong
        when().get("/person/" + personId + "/song/" + anySong).then()
                .statusCode(200)
                .extract().body().as(Person.class);

        List<Song> songsLikedByAlice = when().get("/person/" + personId + "/songs").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Song.class);

        assertThat(songsLikedByAlice).hasSize(1);

    }

    private void populateSongsDb() {

        when().get("/song/new/one/author/metallica").then()
                .statusCode(200);

        when().get("/song/new/Love/author/Colour%20Haze").then()
                .statusCode(200);

        when().get("/song/new/fortress/author/qotsa").then()
                .statusCode(200);
    }

    private void deleteCreatedSongs() {
        when().get("/song/delete/one/author/metallica").then()
                .statusCode(204);

        when().get("/song/delete/Love/author/Colour%20Haze").then()
                .statusCode(204);

        when().get("/song/delete/fortress/author/qotsa").then()
                .statusCode(204);
    }

    private Set<String> getIds(List<Person> people) {
        return people.stream().map(Person::getId).map(Object::toString).collect(Collectors.toSet());
    }
}

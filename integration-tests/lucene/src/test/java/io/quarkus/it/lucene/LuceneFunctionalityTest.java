package io.quarkus.it.lucene;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class LuceneFunctionalityTest {

    @Test
    public void testRAMDirectory() {
        testDirectory(RAMDirectory.class);
    }

    @Test
    public void testByteBufferDirectory() {
        testDirectory(ByteBuffersDirectory.class);
    }

    @Test
    public void testSimpleFSDirectory() {
        testDirectory(SimpleFSDirectory.class);
    }

    @Test
    public void testMMapDirectory() {
        testDirectory(MMapDirectory.class);
    }

    @Test
    public void testNioFSDirectory() {
        testDirectory(NIOFSDirectory.class);
    }

    @Test
    void testQueries() {
        String directoryName = "people";

        createDirectory(directoryName, ByteBuffersDirectory.class);

        Person batman = new PersonBuilder().withName("Bruce Wayne").withAge(45).withMetadata("abcd").withHeight(1.8f)
                .withLatitude(54.2).withLongitude(0.02).getPerson();
        Person hulk = new PersonBuilder().withName("Bruce Banner").withAge(42).withMetadata("abce").withHeight(2.1f)
                .withLatitude(32.2).withLongitude(-0.02).getPerson();

        indexPersons(batman, hulk);

        testQuery("name:bruce", "Bruce Wayne", "Bruce Banner");
        testQuery("name:\"Bruce Wayne\"", "Bruce Wayne");
        testQuery("name:b*", "Bruce Wayne", "Bruce Banner");
        testQuery("age:[40 TO 43]", "Bruce Banner");
        testQuery("name:bru* AND age:[44 TO 46]", "Bruce Wayne");
    }

    private void testQuery(String query, String... expectedNames) {
        List<String> names = search("people", query);
        assertThat(names, equalTo(Arrays.asList(expectedNames)));
    }

    private void testDirectory(Class<?> directoryClass) {
        given().when().post("/lucene/index/my-index?class=" + directoryClass.getName())
                .then().statusCode(200);

        given().when().get("/lucene/index/my-index/files")
                .then().statusCode(200).body(Matchers.equalTo("[]"));

        Person person = new PersonBuilder().withAge(33).withEmail("a@b.com").withEyeDistance(1.5d).setCompany("Red Hat")
                .withHeight(1.7f).withLatitude(0d).withLongitude(0d).withMetadata("metadata").withName("Dummy").getPerson();

        given().when().contentType(APPLICATION_JSON).body(person).post("/lucene/person?index=my-index")
                .then().statusCode(200);

        List<String> files = given().when().get("/lucene/index/my-index/files")
                .then().statusCode(200)
                .extract().body().jsonPath().getList("");

        assertThat(files, hasItem("segments_1"));

        given().when().post("/lucene/index/my-index/forceMerge")
                .then().statusCode(200);
    }

    @AfterEach
    public void cleanAllIndexes() {
        RestAssured.given().delete("/lucene/indexes");
    }

    private void indexPersons(Person... persons) {
        Arrays.stream(persons).forEach(person -> given().when().contentType(APPLICATION_JSON)
                .body(person).post("/lucene/person?index=" + "people").then().statusCode(200));
    }

    private void createDirectory(String name, Class<?> directoryClass) {
        given().when().post("/lucene/index/people?class=" + directoryClass.getName()).then().statusCode(200);
    }

    private List<String> search(String indexName, String query) {
        return given().when().get("/lucene/search?q=" + query + "&index=" + indexName).then().statusCode(200)
                .extract().body().jsonPath().getList("");
    }
}

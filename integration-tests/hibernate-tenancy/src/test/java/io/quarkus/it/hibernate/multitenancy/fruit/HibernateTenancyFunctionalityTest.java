package io.quarkus.it.hibernate.multitenancy.fruit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;

/**
 * Test various Hibernate Multitenancy operations running in Quarkus
 */
@QuarkusTest
public class HibernateTenancyFunctionalityTest {

    private static RestAssuredConfig config;

    @BeforeAll
    public static void beforeClass() {
        config = RestAssured.config().objectMapperConfig(new ObjectMapperConfig(ObjectMapperType.JSONB));
    }

    @BeforeEach
    public void cleanup() {

        deleteIfExists("", "Dragonfruit");
        deleteIfExists("", "Gooseberry");
        deleteIfExists("/mycompany", "Damson");
        deleteIfExists("/mycompany", "Grapefruit");

    }

    @Test
    public void testAddDeleteDefaultTenant() throws Exception {

        // Create fruit for default 'base' tenant
        given().config(config).with().body(new Fruit("Delete")).contentType(ContentType.JSON).when().post("/fruits").then()
                .assertThat().statusCode(is(Status.CREATED.getStatusCode()));

        // Get it
        Fruit newFruit = findByName("", "Delete");

        // Delete it
        given().config(config).pathParam("id", newFruit.getId()).contentType("application/json").accept("application/json")
                .when().delete("/fruits/{id}").then().assertThat().statusCode(is(Status.NO_CONTENT.getStatusCode()));

    }

    @Test
    public void testGetFruitsDefaultTenant() throws Exception {

        Fruit[] fruits = given().config(config).when().get("/fruits").then().assertThat()
                .statusCode(is(Status.OK.getStatusCode())).extract()
                .as(Fruit[].class);
        assertThat(fruits, arrayContaining(new Fruit(2, "Apple"), new Fruit(3, "Banana"), new Fruit(1, "Cherry")));

    }

    @Test
    public void testGetFruitsTenantMycompany() throws Exception {

        Fruit[] fruits = given().config(config).when().get("/mycompany/fruits").then().assertThat()
                .statusCode(is(Status.OK.getStatusCode())).extract()
                .as(Fruit[].class);
        assertThat(fruits, arrayWithSize(3));
        assertThat(fruits, arrayContaining(new Fruit(2, "Apricots"), new Fruit(1, "Avocado"), new Fruit(3, "Blackberries")));

    }

    @Test
    public void testPostFruitDefaultTenant() throws Exception {

        // Create fruit for default 'base' tenant
        Fruit newFruit = new Fruit("Dragonfruit");
        given().config(config).with().body(newFruit).contentType(ContentType.JSON).when().post("/fruits").then()
                .assertThat()
                .statusCode(is(Status.CREATED.getStatusCode()));

        // Getting it directly should return the new fruit
        Fruit dragonFruit = findByName("", newFruit.getName());
        assertThat(dragonFruit, not(is(nullValue())));

        // Getting fruit list should also contain the new fruit
        Fruit[] baseFruits = given().config(config).when().get("/fruits").then().assertThat()
                .statusCode(is(Status.OK.getStatusCode())).extract()
                .as(Fruit[].class);
        assertThat(baseFruits, arrayWithSize(4));
        assertThat(baseFruits,
                arrayContaining(new Fruit(2, "Apple"), new Fruit(3, "Banana"), new Fruit(1, "Cherry"), dragonFruit));

        // The other tenant should NOT have the new fruit
        Fruit[] mycompanyFruits = given().config(config).when().get("/mycompany/fruits").then().assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .extract().as(Fruit[].class);
        assertThat(mycompanyFruits, arrayWithSize(3));
        assertThat(mycompanyFruits,
                arrayContaining(new Fruit(2, "Apricots"), new Fruit(1, "Avocado"), new Fruit(3, "Blackberries")));

        // Getting it directly should also NOT return the new fruit
        assertThat(findByName("/mycompany", newFruit.getName()), is(nullValue()));

    }

    @Test
    public void testUpdateFruitDefaultTenant() throws Exception {

        // Create fruits for both tenants

        Fruit newFruitBase = new Fruit("Dragonfruit");
        given().config(config).with().body(newFruitBase).contentType(ContentType.JSON).when().post("/fruits").then()
                .assertThat()
                .statusCode(is(Status.CREATED.getStatusCode()));
        Fruit baseFruit = findByName("", newFruitBase.getName());
        assertThat(baseFruit, not(is(nullValue())));

        Fruit newFruitMycompany = new Fruit("Damson");
        given().config(config).with().body(newFruitMycompany).contentType(ContentType.JSON).when().post("/mycompany/fruits")
                .then().assertThat()
                .statusCode(is(Status.CREATED.getStatusCode()));
        Fruit mycompanyFruit = findByName("/mycompany", newFruitMycompany.getName());
        assertThat(mycompanyFruit, not(is(nullValue())));

        // Update both

        String baseFruitName = "Gooseberry";
        baseFruit.setName(baseFruitName);
        given().config(config).with().body(baseFruit).contentType(ContentType.JSON).when()
                .put("/fruits/{id}", baseFruit.getId()).then().assertThat()
                .statusCode(is(Status.OK.getStatusCode()));

        String mycompanyFruitName = "Grapefruit";
        mycompanyFruit.setName(mycompanyFruitName);
        given().config(config).with().body(mycompanyFruit).contentType(ContentType.JSON).when()
                .put("/mycompany/fruits/{id}", mycompanyFruit.getId())
                .then().assertThat().statusCode(is(Status.OK.getStatusCode()));

        // Check if we can get them back and they only exist for one tenant

        assertThat(findByName("", baseFruitName), is(not(nullValue())));
        assertThat(findByName("/mycompany", baseFruitName), is(nullValue()));

        assertThat(findByName("/mycompany", mycompanyFruitName), is(not(nullValue())));
        assertThat(findByName("", mycompanyFruitName), is(nullValue()));

    }

    private Fruit findByName(String tenantPath, String name) {
        Response response = given().config(config).when().get(tenantPath + "/fruitsFindBy?type=name&value={name}", name);
        if (response.getStatusCode() == Status.OK.getStatusCode()) {
            return response.as(Fruit.class);
        }
        return null;
    }

    private void deleteIfExists(String tenantPath, String name) {
        Fruit dragonFruit = findByName(tenantPath, name);
        if (dragonFruit != null) {
            given().config(config).pathParam("id", dragonFruit.getId()).when().delete(tenantPath + "/fruits/{id}").then()
                    .assertThat()
                    .statusCode(is(Status.NO_CONTENT.getStatusCode()));
        }
    }

}

package io.quarkus.it.rest.data.hibernate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ParamExtractionTest {

    // --- PageRequest ---

    @Test
    public void testPageRequestDefaults() {
        given()
                .when().get("/echo/page-request")
                .then()
                .statusCode(200)
                .body("page", is(1))
                .body("size", is(10))
                .body("requestTotal", is(true));
    }

    @Test
    public void testPageRequestCustomValues() {
        given()
                .queryParam("page", 3)
                .queryParam("size", 25)
                .queryParam("requestTotal", false)
                .when().get("/echo/page-request")
                .then()
                .statusCode(200)
                .body("page", is(3))
                .body("size", is(25))
                .body("requestTotal", is(false));
    }

    @Test
    public void testPageRequestPartialOverride() {
        given()
                .queryParam("size", 50)
                .when().get("/echo/page-request")
                .then()
                .statusCode(200)
                .body("page", is(1))
                .body("size", is(50))
                .body("requestTotal", is(true));
    }

    // --- Sort ---

    @Test
    public void testSortAscending() {
        given()
                .queryParam("sort", "name")
                .when().get("/echo/sort")
                .then()
                .statusCode(200)
                .body("property", is("name"))
                .body("ascending", is(true));
    }

    @Test
    public void testSortDescending() {
        given()
                .queryParam("sort", "-salary")
                .when().get("/echo/sort")
                .then()
                .statusCode(200)
                .body("property", is("salary"))
                .body("ascending", is(false));
    }

    @Test
    public void testSortAbsent() {
        given()
                .when().get("/echo/sort")
                .then()
                .statusCode(204);
    }

    // --- Order ---

    @Test
    public void testOrderMultiple() {
        given()
                .queryParam("sort", "name")
                .queryParam("sort", "-salary")
                .when().get("/echo/order")
                .then()
                .statusCode(200)
                .body("sorts", hasSize(2))
                .body("sorts[0].property", is("name"))
                .body("sorts[0].ascending", is(true))
                .body("sorts[1].property", is("salary"))
                .body("sorts[1].ascending", is(false));
    }

    @Test
    public void testOrderSingle() {
        given()
                .queryParam("sort", "age")
                .when().get("/echo/order")
                .then()
                .statusCode(200)
                .body("sorts", hasSize(1))
                .body("sorts[0].property", is("age"))
                .body("sorts[0].ascending", is(true));
    }

    @Test
    public void testOrderAbsent() {
        given()
                .when().get("/echo/order")
                .then()
                .statusCode(200)
                .body("sorts", hasSize(0));
    }

    // --- Limit ---

    @Test
    public void testLimitOnly() {
        given()
                .queryParam("limit", 25)
                .when().get("/echo/limit")
                .then()
                .statusCode(200)
                .body("maxResults", is(25))
                .body("startAt", is(1));
    }

    @Test
    public void testLimitWithStartAt() {
        given()
                .queryParam("limit", 10)
                .queryParam("startAt", 51)
                .when().get("/echo/limit")
                .then()
                .statusCode(200)
                .body("maxResults", is(10))
                .body("startAt", is(51));
    }

    @Test
    public void testLimitWithEndAt() {
        given()
                .queryParam("startAt", 51)
                .queryParam("endAt", 60)
                .when().get("/echo/limit")
                .then()
                .statusCode(200)
                .body("maxResults", is(10))
                .body("startAt", is(51));
    }

    @Test
    public void testLimitWithEndAtDefaultStartAt() {
        given()
                .queryParam("endAt", 20)
                .when().get("/echo/limit")
                .then()
                .statusCode(200)
                .body("maxResults", is(20))
                .body("startAt", is(1));
    }

    @Test
    public void testLimitAbsent() {
        given()
                .when().get("/echo/limit")
                .then()
                .statusCode(204);
    }

    // --- Direction ---

    @Test
    public void testDirectionAsc() {
        given()
                .queryParam("direction", "ASC")
                .when().get("/echo/direction")
                .then()
                .statusCode(200)
                .body("direction", is("ASC"));
    }

    @Test
    public void testDirectionDescCaseInsensitive() {
        given()
                .queryParam("direction", "desc")
                .when().get("/echo/direction")
                .then()
                .statusCode(200)
                .body("direction", is("DESC"));
    }

    @Test
    public void testDirectionAbsent() {
        given()
                .when().get("/echo/direction")
                .then()
                .statusCode(204);
    }

    // --- Invalid values ---

    @Test
    public void testPageRequestInvalidPage() {
        given()
                .queryParam("page", "abc")
                .when().get("/echo/page-request")
                .then()
                .statusCode(400);
    }

    @Test
    public void testPageRequestInvalidSize() {
        given()
                .queryParam("size", "xyz")
                .when().get("/echo/page-request")
                .then()
                .statusCode(400);
    }

    @Test
    public void testLimitInvalidValue() {
        given()
                .queryParam("limit", "notanumber")
                .when().get("/echo/limit")
                .then()
                .statusCode(400);
    }

    @Test
    public void testDirectionInvalidValue() {
        given()
                .queryParam("direction", "SIDEWAYS")
                .when().get("/echo/direction")
                .then()
                .statusCode(400);
    }

    // --- Page serialization ---

    @Test
    public void testPageWithTotals() {
        given()
                .when().get("/echo/page")
                .then()
                .statusCode(200)
                .body("content", hasSize(3))
                .body("content[0].name", is("a"))
                .body("content[1].name", is("b"))
                .body("content[2].name", is("c"))
                .body("content", hasSize(3))
                .body("hasNext", is(true))
                .body("hasPrevious", is(false))
                .body("totalElements", is(10))
                .body("totalPages", is(4));
    }

    @Test
    public void testPageWithoutTotals() {
        given()
                .when().get("/echo/page/no-totals")
                .then()
                .statusCode(200)
                .body("content", hasSize(2))
                .body("content", hasSize(2))
                .body("hasNext", is(true))
                .body("hasPrevious", is(true))
                .body("totalElements", nullValue())
                .body("totalPages", nullValue());
    }

    // --- Jackson annotations in Page content ---

    @Test
    public void testJsonPropertyRename() {
        given()
                .when().get("/echo/page/annotated")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].full_name", is("Alice"))
                .body("content[0]", not(hasKey("name")));
    }

    @Test
    public void testJsonIgnore() {
        given()
                .when().get("/echo/page/annotated")
                .then()
                .statusCode(200)
                .body("content[0]", not(hasKey("secret")))
                .body("content[0].age", is(30));
    }
}

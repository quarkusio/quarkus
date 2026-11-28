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

    // --- Custom parameter names with @RestQuery ---

    @Test
    public void testOrderWithCustomParameterName() {
        given()
                .queryParam("orderBy", "name")
                .queryParam("orderBy", "-age")
                .when().get("/echo/order-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("orderBy"))
                .body("sorts", hasSize(2))
                .body("sorts[0].property", is("name"))
                .body("sorts[0].ascending", is(true))
                .body("sorts[1].property", is("age"))
                .body("sorts[1].ascending", is(false));
    }

    @Test
    public void testOrderWithCustomParameterNameAbsent() {
        given()
                .when().get("/echo/order-custom-name")
                .then()
                .statusCode(200)
                .body("message", is("No sorting provided"));
    }

    @Test
    public void testOrderWithCustomParameterNameIgnoresDefaultName() {
        given()
                .queryParam("sort", "name")
                .when().get("/echo/order-custom-name")
                .then()
                .statusCode(200)
                .body("message", is("No sorting provided"));
    }

    @Test
    public void testSortWithCustomParameterName() {
        given()
                .queryParam("sortBy", "name")
                .when().get("/echo/sort-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("sortBy"))
                .body("property", is("name"))
                .body("ascending", is(true));
    }

    @Test
    public void testSortWithCustomParameterNameDescending() {
        given()
                .queryParam("sortBy", "-age")
                .when().get("/echo/sort-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("sortBy"))
                .body("property", is("age"))
                .body("ascending", is(false));
    }

    @Test
    public void testSortWithCustomParameterNameAbsent() {
        given()
                .when().get("/echo/sort-custom-name")
                .then()
                .statusCode(200)
                .body("message", is("No sorting provided"));
    }

    @Test
    public void testSortWithCustomParameterNameIgnoresDefaultName() {
        given()
                .queryParam("sort", "name")
                .when().get("/echo/sort-custom-name")
                .then()
                .statusCode(200)
                .body("message", is("No sorting provided"));
    }

    // --- Additional custom parameter name tests ---

    @Test
    public void testMultipleCustomParametersInSameEndpoint() {
        given()
                .queryParam("orderBy", "name")
                .queryParam("orderBy", "-age")
                .queryParam("filterBy", "status")
                .when().get("/echo/multiple-custom-params")
                .then()
                .statusCode(200)
                .body("ordering", hasSize(2))
                .body("ordering[0].property", is("name"))
                .body("ordering[0].ascending", is(true))
                .body("ordering[1].property", is("age"))
                .body("ordering[1].ascending", is(false))
                .body("filter.property", is("status"))
                .body("filter.ascending", is(true));
    }

    @Test
    public void testMultipleCustomParametersPartial() {
        given()
                .queryParam("orderBy", "name")
                .when().get("/echo/multiple-custom-params")
                .then()
                .statusCode(200)
                .body("ordering", hasSize(1))
                .body("ordering[0].property", is("name"))
                .body("$", not(hasKey("filter")));
    }

    @Test
    public void testMixedDefaultAndCustomParameters() {
        given()
                .queryParam("sort", "name")
                .queryParam("customOrder", "age")
                .when().get("/echo/mixed-params")
                .then()
                .statusCode(200)
                .body("defaultSort.property", is("name"))
                .body("defaultSort.ascending", is(true))
                .body("customOrder", hasSize(1))
                .body("customOrder[0].property", is("age"))
                .body("customOrder[0].ascending", is(true));
    }

    @Test
    public void testMixedParametersOnlyDefault() {
        given()
                .queryParam("sort", "name")
                .when().get("/echo/mixed-params")
                .then()
                .statusCode(200)
                .body("defaultSort.property", is("name"))
                .body("$", not(hasKey("customOrder")));
    }

    @Test
    public void testMixedParametersOnlyCustom() {
        given()
                .queryParam("customOrder", "-salary")
                .when().get("/echo/mixed-params")
                .then()
                .statusCode(200)
                .body("$", not(hasKey("defaultSort")))
                .body("customOrder", hasSize(1))
                .body("customOrder[0].property", is("salary"))
                .body("customOrder[0].ascending", is(false));
    }

    @Test
    public void testParameterNameWithUnderscore() {
        given()
                .queryParam("order_by", "name")
                .when().get("/echo/special-chars")
                .then()
                .statusCode(200)
                .body("parameterName", is("order_by"))
                .body("property", is("name"))
                .body("ascending", is(true));
    }

    @Test
    public void testParameterNameWithUnderscoreDescending() {
        given()
                .queryParam("order_by", "-age")
                .when().get("/echo/special-chars")
                .then()
                .statusCode(200)
                .body("parameterName", is("order_by"))
                .body("property", is("age"))
                .body("ascending", is(false));
    }

    // --- Bare @RestQuery inferring Java parameter name ---

    @Test
    public void testBareRestQueryInfersParameterNameForSort() {
        given()
                .queryParam("sortCriteria", "name")
                .when().get("/echo/bare-query-sort")
                .then()
                .statusCode(200)
                .body("parameterName", is("sortCriteria"))
                .body("property", is("name"))
                .body("ascending", is(true));
    }

    @Test
    public void testBareRestQueryInfersParameterNameForOrder() {
        given()
                .queryParam("orderCriteria", "name")
                .queryParam("orderCriteria", "-age")
                .when().get("/echo/bare-query-order")
                .then()
                .statusCode(200)
                .body("parameterName", is("orderCriteria"))
                .body("sorts", hasSize(2))
                .body("sorts[0].property", is("name"))
                .body("sorts[0].ascending", is(true))
                .body("sorts[1].property", is("age"))
                .body("sorts[1].ascending", is(false));
    }

    @Test
    public void testBareRestQueryInfersParameterNameForDirection() {
        given()
                .queryParam("sortDir", "ASC")
                .when().get("/echo/bare-query-direction")
                .then()
                .statusCode(200)
                .body("parameterName", is("sortDir"))
                .body("direction", is("ASC"));
    }

    @Test
    public void testBareRestQueryInfersParameterNameForLimit() {
        given()
                .queryParam("rowLimit", 25)
                .queryParam("rowLimit_startAt", 10)
                .when().get("/echo/bare-query-limit")
                .then()
                .statusCode(200)
                .body("parameterName", is("rowLimit"))
                .body("maxResults", is(25))
                .body("startAt", is(10));
    }

    @Test
    public void testBareRestQueryInfersParameterNameForPageRequest() {
        given()
                .queryParam("pagination", 3)
                .queryParam("pagination_size", 25)
                .when().get("/echo/bare-query-page-request")
                .then()
                .statusCode(200)
                .body("parameterName", is("pagination"))
                .body("page", is(3))
                .body("size", is(25))
                .body("requestTotal", is(true));
    }

    // --- Empty @RestQuery("") inferring Java parameter name ---

    @Test
    public void testEmptyRestQueryInfersParameterNameForSort() {
        given()
                .queryParam("sortField", "name")
                .when().get("/echo/empty-query-sort")
                .then()
                .statusCode(200)
                .body("parameterName", is("sortField"))
                .body("property", is("name"))
                .body("ascending", is(true));
    }

    @Test
    public void testEmptyRestQueryInfersParameterNameForOrder() {
        given()
                .queryParam("orderFields", "name")
                .queryParam("orderFields", "-age")
                .when().get("/echo/empty-query-order")
                .then()
                .statusCode(200)
                .body("parameterName", is("orderFields"))
                .body("sorts", hasSize(2))
                .body("sorts[0].property", is("name"))
                .body("sorts[0].ascending", is(true))
                .body("sorts[1].property", is("age"))
                .body("sorts[1].ascending", is(false));
    }

    @Test
    public void testEmptyRestQueryInfersParameterNameForDirection() {
        given()
                .queryParam("orderDirection", "DESC")
                .when().get("/echo/empty-query-direction")
                .then()
                .statusCode(200)
                .body("parameterName", is("orderDirection"))
                .body("direction", is("DESC"));
    }

    @Test
    public void testEmptyRestQueryInfersParameterNameForLimit() {
        given()
                .queryParam("maxRecords", 25)
                .queryParam("maxRecords_startAt", 10)
                .when().get("/echo/empty-query-limit")
                .then()
                .statusCode(200)
                .body("parameterName", is("maxRecords"))
                .body("maxResults", is(25))
                .body("startAt", is(10));
    }

    @Test
    public void testEmptyRestQueryInfersParameterNameForPageRequest() {
        given()
                .queryParam("paging", 3)
                .queryParam("paging_size", 25)
                .when().get("/echo/empty-query-page-request")
                .then()
                .statusCode(200)
                .body("parameterName", is("paging"))
                .body("page", is(3))
                .body("size", is(25))
                .body("requestTotal", is(true));
    }

    // --- Explicit custom @RestQuery names for Direction, Limit, PageRequest ---

    @Test
    public void testDirectionWithExplicitCustomName() {
        given()
                .queryParam("sortOrder", "DESC")
                .when().get("/echo/direction-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("sortOrder"))
                .body("direction", is("DESC"));
    }

    @Test
    public void testDirectionWithExplicitCustomNameAbsent() {
        given()
                .when().get("/echo/direction-custom-name")
                .then()
                .statusCode(200)
                .body("message", is("No direction provided"));
    }

    @Test
    public void testLimitWithExplicitCustomName() {
        given()
                .queryParam("maxRows", 50)
                .when().get("/echo/limit-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("maxRows"))
                .body("maxResults", is(50))
                .body("startAt", is(1));
    }

    @Test
    public void testLimitWithExplicitCustomNameAndAuxiliaries() {
        given()
                .queryParam("maxRows", 25)
                .queryParam("maxRows_startAt", 100)
                .when().get("/echo/limit-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("maxRows"))
                .body("maxResults", is(25))
                .body("startAt", is(100));
    }

    @Test
    public void testLimitWithExplicitCustomNameRangeMode() {
        given()
                .queryParam("maxRows_startAt", 50)
                .queryParam("maxRows_endAt", 75)
                .when().get("/echo/limit-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("maxRows"))
                .body("maxResults", is(26))
                .body("startAt", is(50));
    }

    @Test
    public void testPageRequestWithExplicitCustomName() {
        given()
                .queryParam("pageNum", 5)
                .when().get("/echo/page-request-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("pageNum"))
                .body("page", is(5))
                .body("size", is(10))
                .body("requestTotal", is(true));
    }

    @Test
    public void testPageRequestWithExplicitCustomNameAndAuxiliaries() {
        given()
                .queryParam("pageNum", 3)
                .queryParam("pageNum_size", 50)
                .queryParam("pageNum_requestTotal", false)
                .when().get("/echo/page-request-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("pageNum"))
                .body("page", is(3))
                .body("size", is(50))
                .body("requestTotal", is(false));
    }

    // --- Multiple custom-named Limit parameters with independent auxiliaries ---

    @Test
    public void testMultipleLimitsIndependent() {
        given()
                .queryParam("maxRows", 10)
                .queryParam("maxRows_startAt", 100)
                .queryParam("recordLimit", 20)
                .queryParam("recordLimit_startAt", 200)
                .when().get("/echo/multiple-limits")
                .then()
                .statusCode(200)
                .body("maxRows.maxResults", is(10))
                .body("maxRows.startAt", is(100))
                .body("recordLimit.maxResults", is(20))
                .body("recordLimit.startAt", is(200));
    }

    @Test
    public void testMultipleLimitsRangeMode() {
        given()
                .queryParam("maxRows_startAt", 50)
                .queryParam("maxRows_endAt", 75)
                .queryParam("recordLimit_startAt", 100)
                .queryParam("recordLimit_endAt", 150)
                .when().get("/echo/multiple-limits")
                .then()
                .statusCode(200)
                .body("maxRows.maxResults", is(26))
                .body("maxRows.startAt", is(50))
                .body("recordLimit.maxResults", is(51))
                .body("recordLimit.startAt", is(100));
    }

    @Test
    public void testMultipleLimitsPartial() {
        given()
                .queryParam("maxRows", 15)
                .when().get("/echo/multiple-limits")
                .then()
                .statusCode(200)
                .body("maxRows.maxResults", is(15))
                .body("maxRows.startAt", is(1))
                .body("$", not(hasKey("recordLimit")));
    }

    // --- Multiple custom-named PageRequest parameters with independent auxiliaries ---

    @Test
    public void testMultiplePageRequestsIndependent() {
        given()
                .queryParam("mainPage", 2)
                .queryParam("mainPage_size", 25)
                .queryParam("mainPage_requestTotal", false)
                .queryParam("secondPage", 5)
                .queryParam("secondPage_size", 50)
                .queryParam("secondPage_requestTotal", true)
                .when().get("/echo/multiple-page-requests")
                .then()
                .statusCode(200)
                .body("mainPage.page", is(2))
                .body("mainPage.size", is(25))
                .body("mainPage.requestTotal", is(false))
                .body("secondPage.page", is(5))
                .body("secondPage.size", is(50))
                .body("secondPage.requestTotal", is(true));
    }

    @Test
    public void testMultiplePageRequestsPartial() {
        given()
                .queryParam("mainPage", 3)
                .queryParam("mainPage_size", 20)
                .when().get("/echo/multiple-page-requests")
                .then()
                .statusCode(200)
                .body("mainPage.page", is(3))
                .body("mainPage.size", is(20))
                .body("mainPage.requestTotal", is(true))
                .body("$", not(hasKey("secondPage")));
    }

    // --- Custom-named limit alongside default range-based limit ---

    @Test
    public void testMixedLimitsIndependent() {
        given()
                .queryParam("limit", 10)
                .queryParam("startAt", 50)
                .queryParam("maxRows", 20)
                .queryParam("maxRows_startAt", 100)
                .when().get("/echo/mixed-limits")
                .then()
                .statusCode(200)
                .body("defaultLimit.maxResults", is(10))
                .body("defaultLimit.startAt", is(50))
                .body("maxRows.maxResults", is(20))
                .body("maxRows.startAt", is(100));
    }

    @Test
    public void testMixedLimitsDefaultRangeMode() {
        given()
                .queryParam("startAt", 51)
                .queryParam("endAt", 60)
                .queryParam("maxRows", 5)
                .queryParam("maxRows_startAt", 100)
                .when().get("/echo/mixed-limits")
                .then()
                .statusCode(200)
                .body("defaultLimit.maxResults", is(10))
                .body("defaultLimit.startAt", is(51))
                .body("maxRows.maxResults", is(5))
                .body("maxRows.startAt", is(100));
    }

    @Test
    public void testMixedLimitsOnlyDefault() {
        given()
                .queryParam("limit", 30)
                .when().get("/echo/mixed-limits")
                .then()
                .statusCode(200)
                .body("defaultLimit.maxResults", is(30))
                .body("defaultLimit.startAt", is(1))
                .body("$", not(hasKey("maxRows")));
    }

    @Test
    public void testMixedLimitsOnlyCustom() {
        given()
                .queryParam("maxRows", 25)
                .queryParam("maxRows_startAt", 75)
                .when().get("/echo/mixed-limits")
                .then()
                .statusCode(200)
                .body("$", not(hasKey("defaultLimit")))
                .body("maxRows.maxResults", is(25))
                .body("maxRows.startAt", is(75));
    }

    // --- Verify unprefixed auxiliaries do NOT affect custom-named parameters (isolation) ---

    @Test
    public void testCustomLimitIgnoresUnprefixedStartAtEndAt() {
        // This is the CRITICAL test case from the PR review:
        // ?maxRows=5&startAt=51&endAt=60 should produce maxRows=5 starting at 1,
        // NOT a limit of 10 results starting at 51 (which would be from startAt/endAt)
        given()
                .queryParam("maxRows", 5)
                .queryParam("startAt", 51)
                .queryParam("endAt", 60)
                .when().get("/echo/limit-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("maxRows"))
                .body("maxResults", is(5))
                .body("startAt", is(1)); // Should be 1, NOT 51
    }

    @Test
    public void testCustomPageRequestIgnoresUnprefixedSizeRequestTotal() {
        // Custom PageRequest should ignore unprefixed size and requestTotal
        given()
                .queryParam("pageNum", 3)
                .queryParam("size", 100)
                .queryParam("requestTotal", false)
                .when().get("/echo/page-request-custom-name")
                .then()
                .statusCode(200)
                .body("parameterName", is("pageNum"))
                .body("page", is(3))
                .body("size", is(10)) // Should be default 10, NOT 100
                .body("requestTotal", is(true)); // Should be default true, NOT false
    }

    @Test
    public void testMultipleLimitsIsolation() {
        // When both custom and default limit use unprefixed auxiliaries,
        // only the default limit should use them
        given()
                .queryParam("maxRows", 5)
                .queryParam("limit", 10)
                .queryParam("startAt", 100)
                .queryParam("endAt", 150)
                .when().get("/echo/mixed-limits")
                .then()
                .statusCode(200)
                .body("maxRows.maxResults", is(5))
                .body("maxRows.startAt", is(1)) // maxRows ignores unprefixed startAt
                .body("defaultLimit.maxResults", is(51)) // 150-100+1 = 51
                .body("defaultLimit.startAt", is(100)); // default limit uses unprefixed startAt/endAt
    }

    // --- Cases where custom auxiliary parameters are omitted ---

    @Test
    public void testCustomLimitWithoutAuxiliaries() {
        given()
                .queryParam("maxRows", 100)
                .when().get("/echo/limit-custom-name")
                .then()
                .statusCode(200)
                .body("maxResults", is(100))
                .body("startAt", is(1));
    }

    @Test
    public void testCustomPageRequestWithoutAuxiliaries() {
        given()
                .queryParam("pageNum", 7)
                .when().get("/echo/page-request-custom-name")
                .then()
                .statusCode(200)
                .body("page", is(7))
                .body("size", is(10))
                .body("requestTotal", is(true));
    }

    @Test
    public void testMultipleLimitsOneWithoutAuxiliaries() {
        given()
                .queryParam("maxRows", 15)
                .queryParam("recordLimit", 25)
                .queryParam("recordLimit_startAt", 50)
                .when().get("/echo/multiple-limits")
                .then()
                .statusCode(200)
                .body("maxRows.maxResults", is(15))
                .body("maxRows.startAt", is(1))
                .body("recordLimit.maxResults", is(25))
                .body("recordLimit.startAt", is(50));
    }

    // --- @QueryParam annotation support ---

    @Test
    public void testSortWithQueryParam() {
        given()
                .queryParam("sortBy", "name")
                .when().get("/echo/sort-query-param")
                .then()
                .statusCode(200)
                .body("parameterName", is("sortBy"))
                .body("property", is("name"))
                .body("ascending", is(true));
    }

    @Test
    public void testSortWithQueryParamDescending() {
        given()
                .queryParam("sortBy", "-age")
                .when().get("/echo/sort-query-param")
                .then()
                .statusCode(200)
                .body("parameterName", is("sortBy"))
                .body("property", is("age"))
                .body("ascending", is(false));
    }

    @Test
    public void testSortWithQueryParamAbsent() {
        given()
                .when().get("/echo/sort-query-param")
                .then()
                .statusCode(200)
                .body("message", is("No sorting provided"));
    }

    @Test
    public void testOrderWithQueryParam() {
        given()
                .queryParam("orderBy", "name")
                .queryParam("orderBy", "-age")
                .when().get("/echo/order-query-param")
                .then()
                .statusCode(200)
                .body("parameterName", is("orderBy"))
                .body("sorts", hasSize(2))
                .body("sorts[0].property", is("name"))
                .body("sorts[0].ascending", is(true))
                .body("sorts[1].property", is("age"))
                .body("sorts[1].ascending", is(false));
    }

    @Test
    public void testOrderWithQueryParamAbsent() {
        given()
                .when().get("/echo/order-query-param")
                .then()
                .statusCode(200)
                .body("message", is("No ordering provided"));
    }

    @Test
    public void testLimitWithQueryParam() {
        given()
                .queryParam("maxRows", 50)
                .when().get("/echo/limit-query-param")
                .then()
                .statusCode(200)
                .body("parameterName", is("maxRows"))
                .body("maxResults", is(50))
                .body("startAt", is(1));
    }

    @Test
    public void testLimitWithQueryParamAndAuxiliaries() {
        given()
                .queryParam("maxRows", 25)
                .queryParam("maxRows_startAt", 100)
                .when().get("/echo/limit-query-param")
                .then()
                .statusCode(200)
                .body("parameterName", is("maxRows"))
                .body("maxResults", is(25))
                .body("startAt", is(100));
    }

    @Test
    public void testLimitWithQueryParamAbsent() {
        given()
                .when().get("/echo/limit-query-param")
                .then()
                .statusCode(200)
                .body("message", is("No limit provided"));
    }
}

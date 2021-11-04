package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public class QueryReaderTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Simple.class, Nested.class, NestedCollection.class, QueryFunction.class));

    @Test
    public void testSimple() throws Exception {
        RestAssured.given()
                .queryParam("intVal", 42)
                .queryParam("shortVal", 4)
                .queryParam("longVal", 442)
                .queryParam("doubleVal", "4.2")
                .queryParam("floatVal", "4.2")
                .queryParam("b", "1")
                .queryParam("boolVal", "true")
                .queryParam("value", "hello world")
                .get("/simple")
                .then().statusCode(200)
                .defaultParser(Parser.JSON)
                .body("intVal", equalTo(42))
                .body("shortVal", equalTo(4))
                .body("longVal", equalTo(442))
                .body("floatVal", equalTo(4.2f))
                .body("doubleVal", equalTo(4.2f))
                .body("b", equalTo(1))
                .body("boolVal", equalTo(true))
                .body("value", equalTo("hello world"));
    }

    @Test
    public void testNested() throws Exception {
        RestAssured.given()
                .queryParam("nestedOne.intVal", 42)
                .queryParam("nestedOne.shortVal", 4)
                .queryParam("nestedOne.longVal", 442)
                .queryParam("nestedOne.doubleVal", "4.2")
                .queryParam("nestedOne.floatVal", "4.2")
                .queryParam("nestedOne.b", "1")
                .queryParam("nestedOne.boolVal", "true")
                .queryParam("nestedOne.value", "hello world")
                .queryParam("nestedTwo.intVal", 32)
                .queryParam("nestedTwo.shortVal", 3)
                .queryParam("nestedTwo.longVal", 332)
                .queryParam("nestedTwo.doubleVal", "3.2")
                .queryParam("nestedTwo.floatVal", "3.2")
                .queryParam("nestedTwo.b", "2")
                .queryParam("nestedTwo.boolVal", "true")
                .queryParam("nestedTwo.value", "hello world too")
                .get("/nested")
                .then().statusCode(200)
                .defaultParser(Parser.JSON)
                .body("nestedOne.intVal", equalTo(42))
                .body("nestedOne.shortVal", equalTo(4))
                .body("nestedOne.longVal", equalTo(442))
                .body("nestedOne.floatVal", equalTo(4.2f))
                .body("nestedOne.doubleVal", equalTo(4.2f))
                .body("nestedOne.b", equalTo(1))
                .body("nestedOne.boolVal", equalTo(true))
                .body("nestedOne.value", equalTo("hello world"))
                .body("nestedTwo.intVal", equalTo(32))
                .body("nestedTwo.shortVal", equalTo(3))
                .body("nestedTwo.longVal", equalTo(332))
                .body("nestedTwo.floatVal", equalTo(3.2f))
                .body("nestedTwo.doubleVal", equalTo(3.2f))
                .body("nestedTwo.b", equalTo(2))
                .body("nestedTwo.boolVal", equalTo(true))
                .body("nestedTwo.value", equalTo("hello world too"));
    }

    @Test
    public void testNestedCollection() throws Exception {
        RestAssured.given()
                .queryParam("intMap.one", 1)
                .queryParam("intMap.two", 2)
                .queryParam("intKeyMap.1", "one")
                .queryParam("intKeyMap.2", "two")
                .queryParam("stringList", "one")
                .queryParam("stringList", "two")
                .get("/nestedCollection")
                .then().statusCode(200)
                .defaultParser(Parser.JSON)
                .body("intMap.one", equalTo(1))
                .body("intMap.two", equalTo(2))
                .body("stringList[0]", equalTo("one"))
                .body("stringList[1]", equalTo("two"))
                .body("intKeyMap.1", equalTo("one"))
                .body("intKeyMap.1", equalTo("one"));

        RestAssured.given()
                .queryParam("simpleMap.one.value", "one")
                .queryParam("simpleMap.two.value", "two")
                .queryParam("simpleMap.one.intVal", 1)
                .queryParam("simpleMap.two.intVal", 2)
                .queryParam("simpleList.one.value", "one")
                .queryParam("simpleList.two.value", "two")
                .queryParam("simpleList.one.intVal", 1)
                .queryParam("simpleList.two.intVal", 2)
                .get("/nestedCollection")
                .then().statusCode(200)
                .defaultParser(Parser.JSON)
                .body("simpleMap.one.intVal", equalTo(1))
                .body("simpleMap.two.intVal", equalTo(2))
                .body("simpleMap.one.value", equalTo("one"))
                .body("simpleMap.two.value", equalTo("two"))
                .body("simpleList[0].intVal", equalTo(1))
                .body("simpleList[1].intVal", equalTo(2))
                .body("simpleList[0].value", equalTo("one"))
                .body("simpleList[1].value", equalTo("two"));

    }

}

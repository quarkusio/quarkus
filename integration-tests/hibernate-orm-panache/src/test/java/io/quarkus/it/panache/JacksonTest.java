package io.quarkus.it.panache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JacksonTest {

    @Test
    public void testJsonIgnoreHonoured() {
        List<Book> books = RestAssured.when().get("/book/Berlin/Beevor").then().extract().body().jsonPath().getList(".",
                Book.class);

        assertThat(books).hasSize(1).filteredOn(book -> book.author != null).isEmpty();
    }
}

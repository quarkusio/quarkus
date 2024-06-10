package io.quarkus.spring.data.rest.paged;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItems;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Link;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.spring.data.rest.AbstractEntity;
import io.quarkus.spring.data.rest.CrudAndPagedRecordsRepository;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.Header;
import io.restassured.http.Headers;

class DefaultPagedResourceBisTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractEntity.class, Record.class, CrudAndPagedRecordsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    //    @Disabled
    void shouldListHal() {
        given().accept("application/hal+json")
                .when().get("/crud-and-paged-records")
                .then().statusCode(200).log().all()
                .and().body("_embedded.crud-and-paged-records.id", hasItems(1, 2))
                .and().body("_embedded.crud-and-paged-records.name", hasItems("first", "second"))
                .and()
                .body("_embedded.crud-and-paged-records._links.add.href",
                        hasItems(endsWith("/crud-and-paged-records"), endsWith("/crud-and-paged-records")))
                .and()
                .body("_embedded.crud-and-paged-records._links.list.href",
                        hasItems(endsWith("/crud-and-paged-records"), endsWith("/crud-and-paged-records")))
                .and()
                .body("_embedded.crud-and-paged-records._links.self.href",
                        hasItems(endsWith("/crud-and-paged-records/1"), endsWith("/crud-and-paged-records/2")))
                .and()
                .body("_embedded.crud-and-paged-records._links.update.href",
                        hasItems(endsWith("/crud-and-paged-records/1"), endsWith("/crud-and-paged-records/2")))
                .and()
                .body("_embedded.crud-and-paged-records._links.remove.href",
                        hasItems(endsWith("/crud-and-paged-records/1"), endsWith("/crud-and-paged-records/2")))
                .and().body("_links.add.href", endsWith("/crud-and-paged-records"))
                .and().body("_links.list.href", endsWith("/crud-and-paged-records"))
                .and().body("_links.first.href", endsWith("/crud-and-paged-records?page=0&size=20"))
                .and().body("_links.last.href", endsWith("/crud-and-paged-records?page=0&size=20"));
    }

    private void assertLinks(Headers headers, Map<String, String> expectedLinks) {
        List<Link> links = new LinkedList<>();
        for (Header header : headers.getList("Link")) {
            links.add(Link.valueOf(header.getValue()));
        }
        assertThat(links).hasSize(expectedLinks.size());
        for (Map.Entry<String, String> expectedLink : expectedLinks.entrySet()) {
            assertThat(links).anySatisfy(link -> {
                assertThat(link.getUri().toString()).endsWith(expectedLink.getValue());
                assertThat(link.getRel()).isEqualTo(expectedLink.getKey());
            });
        }
    }
}

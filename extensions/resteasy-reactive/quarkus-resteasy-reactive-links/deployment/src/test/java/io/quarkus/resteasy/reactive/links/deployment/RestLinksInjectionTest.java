package io.quarkus.resteasy.reactive.links.deployment;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestLinksInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractEntity.class, TestRecord.class, TestResource.class));

    @TestHTTPResource("records")
    String recordsUrl;

    @TestHTTPResource("records/without-links")
    String recordsWithoutLinksUrl;

    @Test
    void shouldGetById() {
        List<String> firstRecordLinks = when().get(recordsUrl + "/1")
                .thenReturn()
                .getHeaders()
                .getValues("Link");
        assertThat(firstRecordLinks).containsOnly(
                Link.fromUri(recordsUrl).rel("list").build().toString(),
                Link.fromUri(recordsWithoutLinksUrl).rel("getAllWithoutLinks").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/1")).rel("self").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/first")).rel("getBySlug").build().toString());

        List<String> secondRecordLinks = when().get(recordsUrl + "/2")
                .thenReturn()
                .getHeaders()
                .getValues("Link");
        assertThat(secondRecordLinks).containsOnly(
                Link.fromUri(recordsUrl).rel("list").build().toString(),
                Link.fromUri(recordsWithoutLinksUrl).rel("getAllWithoutLinks").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/2")).rel("self").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/second"))
                        .rel("getBySlug")
                        .build()
                        .toString());
    }

    @Test
    void shouldGetBySlug() {
        List<String> firstRecordLinks = when().get(recordsUrl + "/first")
                .thenReturn()
                .getHeaders()
                .getValues("Link");
        assertThat(firstRecordLinks).containsOnly(
                Link.fromUri(recordsUrl).rel("list").build().toString(),
                Link.fromUri(recordsWithoutLinksUrl).rel("getAllWithoutLinks").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/1")).rel("self").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/first")).rel("getBySlug").build().toString());

        List<String> secondRecordLinks = when().get(recordsUrl + "/second")
                .thenReturn()
                .getHeaders()
                .getValues("Link");
        assertThat(secondRecordLinks).containsOnly(
                Link.fromUri(recordsUrl).rel("list").build().toString(),
                Link.fromUri(recordsWithoutLinksUrl).rel("getAllWithoutLinks").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/2")).rel("self").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/second"))
                        .rel("getBySlug")
                        .build()
                        .toString());
    }

    @Test
    void shouldGetAll() {
        List<String> links = when().get(recordsUrl)
                .thenReturn()
                .getHeaders()
                .getValues("Link");
        assertThat(links).containsOnly(
                Link.fromUri(recordsUrl).rel("list").build().toString(),
                Link.fromUri(recordsWithoutLinksUrl).rel("getAllWithoutLinks").build().toString());
    }

    @Test
    void shouldGetAllWithoutLinks() {
        List<String> links = when().get(recordsWithoutLinksUrl)
                .thenReturn()
                .getHeaders()
                .getValues("Link");
        assertThat(links).isEmpty();
    }
}

package io.quarkus.resteasy.reactive.links.deployment;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * The Jakarta REST annotations live on an interface, while {@link RestLink} and {@link InjectRestLinks} are placed on
 * the overriding methods of the implementation.
 */
public class RestLinksOnOverridingMethodTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractId.class, AbstractEntity.class, TestRecord.class, RecordsApi.class,
                            RecordsResource.class));

    @TestHTTPResource("api-records")
    String recordsUrl;

    @Test
    void linksDeclaredOnTheImplementationAreInjected() {
        List<String> links = when().get(recordsUrl + "/1")
                .thenReturn()
                .getHeaders()
                .getValues("Link");
        assertThat(links).containsOnly(
                Link.fromUri(recordsUrl).rel("list").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/1")).rel("self").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/by-slug/first")).rel("get-by-slug").build()
                        .toString());
    }

    @Test
    void linksDeclaredOnTheInterfaceAreStillInjected() {
        List<String> links = when().get(recordsUrl + "/by-slug/first")
                .thenReturn()
                .getHeaders()
                .getValues("Link");
        assertThat(links).containsOnly(
                Link.fromUri(recordsUrl).rel("list").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/1")).rel("self").build().toString(),
                Link.fromUriBuilder(UriBuilder.fromUri(recordsUrl).path("/by-slug/first")).rel("get-by-slug").build()
                        .toString());
    }

    @Path("/api-records")
    public interface RecordsApi {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        List<TestRecord> getAll();

        @GET
        @Path("/{id: \\d+}")
        @Produces(MediaType.APPLICATION_JSON)
        TestRecord getById(@PathParam("id") int id);

        @GET
        @Path("/by-slug/{slug}")
        @Produces(MediaType.APPLICATION_JSON)
        @RestLink(rel = "get-by-slug")
        @InjectRestLinks(RestLinkType.INSTANCE)
        TestRecord getBySlug(@PathParam("slug") String slug);
    }

    public static class RecordsResource implements RecordsApi {

        private static final List<TestRecord> RECORDS = List.of(new TestRecord(1, "first", "First value"),
                new TestRecord(2, "second", "Second value"));

        @RestLink(entityType = TestRecord.class)
        @InjectRestLinks
        @Override
        public List<TestRecord> getAll() {
            return RECORDS;
        }

        @RestLink(entityType = TestRecord.class)
        @InjectRestLinks(RestLinkType.INSTANCE)
        @Override
        public TestRecord getById(int id) {
            return RECORDS.stream()
                    .filter(record -> record.getId() == id)
                    .findFirst()
                    .orElseThrow(NotFoundException::new);
        }

        @Override
        public TestRecord getBySlug(String slug) {
            return RECORDS.stream()
                    .filter(record -> record.getSlug().equals(slug))
                    .findFirst()
                    .orElseThrow(NotFoundException::new);
        }
    }
}

package io.quarkus.resteasy.reactive.links.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.reactive.common.util.RestMediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.response.Response;

public class HalWrapperResourceTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HalWrapperResource.class, TestRecordWithIdAndPersistenceIdAndRestLinkId.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-resteasy-reactive-jackson", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-hal", Version.getVersion())));

    @TestHTTPResource("hal")
    String recordsUrl;

    @TestHTTPResource("hal/{id}")
    String recordIdUrl;

    @Test
    void shouldGetAllRecordsWithCustomHalMetadata() {
        Response response = given()
                .header(HttpHeaders.ACCEPT, RestMediaType.APPLICATION_HAL_JSON)
                .get(recordsUrl).thenReturn();

        assertThat(response.body().jsonPath().getString("_embedded['collectionName'][0].restLinkId")).isEqualTo("1");
        assertThat(response.body().jsonPath().getString("_embedded['collectionName'][0]._links.self.href")).endsWith("/hal/1");
        assertThat(response.body().jsonPath().getString("_embedded['collectionName'][0]._links.list.href")).endsWith("/hal");
        assertThat(response.body().jsonPath().getString("_embedded['collectionName'][1].restLinkId")).isEqualTo("2");
        assertThat(response.body().jsonPath().getString("_embedded['collectionName'][1]._links.self.href")).endsWith("/hal/2");
        assertThat(response.body().jsonPath().getString("_embedded['collectionName'][1]._links.list.href")).endsWith("/hal");
        assertThat(response.body().jsonPath().getString("_links.first-record.href")).endsWith("/hal/1");
        assertThat(response.body().jsonPath().getString("_links.list.href")).endsWith("/hal");
    }

    @Test
    void shouldGetSingleRecordWithCustomHalMetadata() {
        Response response = given()
                .header(HttpHeaders.ACCEPT, RestMediaType.APPLICATION_HAL_JSON)
                .get(recordIdUrl, 1L)
                .thenReturn();

        assertThat(response.body().jsonPath().getString("restLinkId")).isEqualTo("1");
        assertThat(response.body().jsonPath().getString("_links.parent-record.href")).endsWith("/hal/1/parent");
        assertThat(response.body().jsonPath().getString("_links.self.href")).endsWith("/hal/1");
        assertThat(response.body().jsonPath().getString("_links.list.href")).endsWith("/hal");

    }
}

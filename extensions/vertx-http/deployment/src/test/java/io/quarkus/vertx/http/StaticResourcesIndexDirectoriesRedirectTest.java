package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class StaticResourcesIndexDirectoriesRedirectTest extends AbstractStaticResourcesIndexDirectoriesTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = test("redirect");

    @Test
    public void directoryWithoutTrailingSlashIsRedirected() {
        given().config(NO_REDIRECTS).get("/classpath").then().statusCode(301).header("Location", endsWith("/classpath/"));
        given().config(NO_REDIRECTS).get("/generated").then().statusCode(301).header("Location", endsWith("/generated/"));
        given().config(NO_REDIRECTS).get("/classpath?q=1").then().statusCode(301)
                .header("Location", endsWith("/classpath/?q=1"));
    }

    /**
     * A {@code Location} starting with two slashes is a network-path reference: a browser reads what follows as the
     * host and leaves the application. The request path is never reflected into the header unless it is an absolute
     * path denoting exactly the directory that was matched.
     */
    @Test
    public void aPathThatCouldBeReadAsAnAuthorityIsNotRedirected() throws IOException {
        assertThat(responseHead("//classpath")).startsWith("HTTP/1.1 404");
        assertThat(responseHead("///classpath")).startsWith("HTTP/1.1 404");
    }

    @Test
    public void aPathThatIsNotInItsCanonicalFormIsNotRedirected() throws IOException {
        assertThat(responseHead("/./classpath")).startsWith("HTTP/1.1 404");
        assertThat(responseHead("/generated/../classpath")).startsWith("HTTP/1.1 404");
    }

    @Test
    public void anEncodedPathDenotingTheDirectoryIsStillRedirected() throws IOException {
        assertThat(responseHead("/%63lasspath"))
                .startsWith("HTTP/1.1 301")
                .contains("location: /%63lasspath/");
    }
}

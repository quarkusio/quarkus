{#if input.extra-codestarts.contains("integration-tests")}
package {package-name}.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

public class {class-name-base}CodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
        .languages(Language.JAVA)
        .setupStandaloneExtensionTest("{group-id}:{namespace.id}{extension.id}")
        .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.{class-name-base}Resource");
    }

    @Test
    void buildAllProjects() throws Throwable {
        codestartTest.buildAllProjects();
    }
}

{#else}
<SKIP>
{/if}
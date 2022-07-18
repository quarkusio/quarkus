{#if input.extra-codestarts.contains("integration-tests")}
package {package-name}.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class {class-name-base}CodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
        .languages(Language.JAVA)
        .setupStandaloneExtensionTest("{group-id}:{namespace.id}{extension.id}")
        .build();

    /**
     *  Make sure the generated code meets the expectations.
     *  <br>
     *  The generated code uses mocked data to be immutable and allow snapshot testing.
     *  <br><br>
     *
     *  Read the doc: <br>
     *  {@link https://quarkus.io/guides/extension-codestart#integration-test}
     */
    @Test
    void testContent() throws Throwable {
        //codestartTest.checkGeneratedSource("org.acme.SomeClass");
        //codestartTest.assertThatGeneratedFileMatchSnapshot(Language.JAVA, "\"src/main/resources/some-resource.ext");
    }

    /**
     * This test runs the build (with tests) on generated projects for all selected languages
     */
    @Test
    void buildAllProjects() throws Throwable {
        codestartTest.buildAllProjects();
    }
}

{#else}
<SKIP>
{/if}
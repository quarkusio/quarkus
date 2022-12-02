package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithConflictingEnvTest {
    private static final String APPLICATION_NAME = "conflicting";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APPLICATION_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .assertBuildException(e -> assertThat(e)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(
                            "- 'envvar': first defined as 'envvar' env var with value 'value' redefined as 'envvar' env var with value from field 'field'"))
            .withConfigurationResource("kubernetes-with-" + APPLICATION_NAME + "-env.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void buildShouldFail() throws IOException {
        fail("Build should have failed and therefore this method should not have been called");
    }
}

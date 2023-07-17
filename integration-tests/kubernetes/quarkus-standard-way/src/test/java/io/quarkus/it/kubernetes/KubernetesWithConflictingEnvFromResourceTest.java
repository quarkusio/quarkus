package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithConflictingEnvFromResourceTest {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationVersion("0.1-SNAPSHOT")
            .assertBuildException(e -> assertThat(e)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(
                            "'db-password' env var can't simultaneously take its value from 'db-configmap' configmap & 'db-secret' secret"))
            .withConfigurationResource("kubernetes-with-conflicting-env-from-resource.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void buildShouldFail() throws IOException {
        fail("Build should have failed and therefore this method should not have been called");
    }
}

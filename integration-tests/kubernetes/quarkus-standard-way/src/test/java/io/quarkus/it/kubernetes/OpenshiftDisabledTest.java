package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftDisabledTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift-disabled")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.deployment-target", "openshift")
            .overrideConfigKey("quarkus.kubernetes.enabled", "false");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        assertThat(prodModeTestResults.getBuildDir())
                .isDirectoryNotContaining(p -> p.toString().endsWith("kubernetes"));
    }

}

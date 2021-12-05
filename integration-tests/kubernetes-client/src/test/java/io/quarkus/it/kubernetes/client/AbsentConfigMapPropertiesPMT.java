package io.quarkus.it.kubernetes.client;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(value = CustomKubernetesMockServerTestResource.class, restrictToAnnotatedClass = true)
public class AbsentConfigMapPropertiesPMT {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(ConfigMapProperties.class))
            .setApplicationName("k8s-configMaps")
            .withConfigurationResource("application-demo.properties")
            .setRun(true)
            .setExpectExit(true)
            .setApplicationVersion("0.1-SNAPSHOT");

    @Test
    public void startUpShouldFail() throws IOException {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("ConfigMap 'cmap4' not found in namespace 'demo'").contains("RuntimeException");
    }

}

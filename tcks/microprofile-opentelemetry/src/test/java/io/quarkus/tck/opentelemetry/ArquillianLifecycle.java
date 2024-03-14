package io.quarkus.tck.opentelemetry;

import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;

public class ArquillianLifecycle {
    public void afterDeploy(@Observes AfterDeploy event, TestClass testClass) {
        // The TCK expects the url to end with a slash
        System.setProperty("test.url", System.getProperty("test.url") + "/");
    }
}

package io.quarkus.tck.opentelemetry;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class DeploymentProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            WebArchive war = (WebArchive) archive;

            boolean otelSdkDisabled = true;
            Node microprofileConfig = war.get("/WEB-INF/classes/META-INF/microprofile-config.properties");
            if (microprofileConfig != null) {
                Properties properties = new Properties();
                try (InputStreamReader reader = new InputStreamReader(microprofileConfig.getAsset().openStream(), UTF_8)) {
                    properties.load(reader);
                    otelSdkDisabled = Boolean.parseBoolean(properties.getProperty("otel.sdk.disabled", "true"));
                } catch (IOException e) {
                    // Ignore
                }
            }

            war.addAsResource(new StringAsset(
                    "quarkus.otel.sdk.disabled=" + (otelSdkDisabled ? "true" : "false") + "\n"),
                    "application.properties");

            war.addClass(ExecutorProvider.class);
        }
    }
}

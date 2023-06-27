package io.quarkus.test.devui;

import java.util.Iterator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIArcJsonRPCTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class).addAsResource(new StringAsset("quarkus.arc.dev-mode.monitoring-enabled=true"),
                            "application.properties"));

    public DevUIArcJsonRPCTest() {
        super("io.quarkus.quarkus-arc");
    }

    @Test
    public void testEvents() throws Exception {
        JsonNode events = super.executeJsonRPCMethod("getLastEvents");
        Assertions.assertNotNull(events);
        Assertions.assertTrue(events.isArray());

        Iterator<JsonNode> en = events.elements();
        boolean startupExists = false;
        while (en.hasNext()) {
            JsonNode event = en.next();
            String type = event.get("type").asText();
            if (type.equals("io.quarkus.runtime.StartupEvent")) {
                startupExists = true;
                break;
            }
        }

        Assertions.assertTrue(startupExists);
    }

    @Test
    public void testInvocations() throws Exception {
        JsonNode invocations = super.executeJsonRPCMethod("getLastInvocations");
        Assertions.assertNotNull(invocations);
        Assertions.assertTrue(invocations.isArray());

        Iterator<JsonNode> en = invocations.elements();
        boolean loggerExists = false;
        while (en.hasNext()) {
            JsonNode invocation = en.next();
            String methodName = invocation.get("methodName").asText();
            if (methodName.equals("io.quarkus.arc.runtime.LoggerProducer#getSimpleLogger")) {
                loggerExists = true;
                break;
            }
        }

        Assertions.assertTrue(loggerExists);
    }

    @Named
    @ApplicationScoped
    public static class Foo {

        void onStr(@Observes String event) {
        }

    }
}

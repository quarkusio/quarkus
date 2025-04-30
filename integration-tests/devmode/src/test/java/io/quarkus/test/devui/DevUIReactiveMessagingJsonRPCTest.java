package io.quarkus.test.devui;

import java.util.Iterator;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIReactiveMessagingJsonRPCTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(MyProcessor.class, DummyConnector.class)
                    .addAsResource(
                            new StringAsset(
                                    "mp.messaging.incoming.input.connector=dummy\n"
                                            + "mp.messaging.incoming.input.values=hallo"),
                            "application.properties"));

    public DevUIReactiveMessagingJsonRPCTest() {
        super("io.quarkus.quarkus-messaging");
    }

    @Test
    public void testProcessor() throws Exception {
        JsonNode info = super.executeJsonRPCMethod("getInfo");
        Assertions.assertNotNull(info);
        Assertions.assertTrue(info.isArray());

        Iterator<JsonNode> en = info.elements();
        boolean consumerExists = false;
        boolean publisherExists = false;
        while (en.hasNext()) {
            JsonNode channel = en.next();
            JsonNode consumers = channel.get("consumers");
            if (consumers != null) {
                consumerExists = typeAndDescriptionExist(consumers, "CHANNEL",
                        "<code>io.quarkus.test.devui.MyProcessor#channel</code>");
            }
            JsonNode publishers = channel.get("publishers");
            if (publishers != null) {
                publisherExists = typeAndDescriptionExist(publishers, "PROCESSOR",
                        "<code>io.quarkus.test.devui.MyProcessor#process()</code>");
            }
        }

        Assertions.assertTrue(consumerExists);
        Assertions.assertTrue(publisherExists);

    }

    private boolean typeAndDescriptionExist(JsonNode a, String type, String description) {
        if (a.isArray()) {
            Iterator<JsonNode> en = a.elements();
            while (en.hasNext()) {
                JsonNode b = en.next();
                if (isTypeAndDescription(b, type, description)) {
                    return true;
                }
            }
        } else {
            return isTypeAndDescription(a, type, description);
        }
        return false;
    }

    private boolean isTypeAndDescription(JsonNode b, String type, String description) {
        String t = b.get("type").asText();
        String d = b.get("description").asText();
        if (t.equals(type) &&
                d.equals(description)) {
            return true;
        }
        return false;
    }

}

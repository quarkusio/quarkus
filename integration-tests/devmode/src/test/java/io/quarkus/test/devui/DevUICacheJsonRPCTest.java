package io.quarkus.test.devui;

import java.util.Iterator;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.cache.CacheResult;
import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUICacheJsonRPCTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(MyBean.class));

    public DevUICacheJsonRPCTest() {
        super("quarkus-cache");
    }

    @Test
    public void testCaches() throws Exception {
        JsonNode all = super.executeJsonRPCMethod("getAll");
        Assertions.assertNotNull(all);
        Assertions.assertTrue(all.isArray());

        Iterator<JsonNode> en = all.elements();
        boolean exists = false;
        while (en.hasNext()) {
            JsonNode cache = en.next();
            String name = cache.get("name").asText();
            if (name.equals("myCache")) {
                exists = true;
                break;
            }
        }
        Assertions.assertTrue(exists);
    }

    @Singleton
    public static class MyBean {

        @CacheResult(cacheName = "myCache")
        String ping() {
            return "foo";
        }

    }

}

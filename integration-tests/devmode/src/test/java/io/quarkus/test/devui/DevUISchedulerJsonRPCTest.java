package io.quarkus.test.devui;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUISchedulerJsonRPCTest extends DevUIJsonRPCTest {

    private static final String METHOD_DESCRIPTION = "io.quarkus.test.devui.DevUISchedulerJsonRPCTest$Jobs#run";

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(Jobs.class));

    public DevUISchedulerJsonRPCTest() {
        super("io.quarkus.quarkus-scheduler");
    }

    @Test
    public void testScheduler() throws Exception {

        JsonNode data = super.executeJsonRPCMethod("getData");
        Assertions.assertNotNull(data);
        Assertions.assertTrue(data.get("schedulerRunning").asBoolean());

        JsonNode methods = data.get("methods");
        Assertions.assertNotNull(methods);
        Assertions.assertTrue(methods.isArray());

        Iterator<JsonNode> en = methods.elements();
        boolean exists = false;
        while (en.hasNext()) {
            JsonNode method = en.next();
            String methodDescription = method.get("methodDescription").asText();
            if (methodDescription.equals(METHOD_DESCRIPTION)) {
                exists = true;
                break;
            }
        }

        Assertions.assertTrue(exists);
    }

    @Test
    public void testExecuteJob() throws Exception {
        JsonNode result = super.executeJsonRPCMethod("executeJob", Map.of("methodDescription", METHOD_DESCRIPTION));
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.get("success").asBoolean());
    }

    public static class Jobs {

        @Scheduled(every = "2h", delay = 2, delayUnit = TimeUnit.HOURS)
        public void run() {
        }

    }

}

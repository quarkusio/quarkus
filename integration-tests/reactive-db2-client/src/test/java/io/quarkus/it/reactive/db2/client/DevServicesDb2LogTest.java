package io.quarkus.it.reactive.db2.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.LogCollectingTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that Dev Services for DB2 starts without errors.
 * Note LogCollectingTestResource cannot be used in native mode,
 * hence the lack of a corresponding native mode test.
 */
@QuarkusTest
@QuarkusTestResource(value = LogCollectingTestResource.class, restrictToAnnotatedClass = true, initArgs = {
        @ResourceArg(name = LogCollectingTestResource.LEVEL, value = "WARNING"),
        @ResourceArg(name = LogCollectingTestResource.INCLUDE, value = "io\\.quarkus\\.devservices\\.db2\\..*")
})
public class DevServicesDb2LogTest {
    @Test
    public void testNoErrorsOnStartup() {
        assertThat(LogCollectingTestResource.current().getRecords())
                .as("Dev Services DB2 logs")
                .extracting(LogCollectingTestResource::format)
                // No errors should occur during DB2 startup
                .noneMatch(log -> log.contains("ERROR"));
    }
}

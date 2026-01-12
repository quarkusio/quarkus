package io.quarkus.it.reactive.db2.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.LogCollectingTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that Dev Services for DB2 correctly handles datasource names
 * longer than 8 characters (DB2's database name limit).
 * <p>
 * Note LogCollectingTestResource cannot be used in native mode,
 * hence the lack of a corresponding native mode test.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51225">GitHub Issue #51225</a>
 */
@QuarkusTest
@QuarkusTestResource(value = LogCollectingTestResource.class, restrictToAnnotatedClass = true, initArgs = {
        @ResourceArg(name = LogCollectingTestResource.LEVEL, value = "WARNING"),
        @ResourceArg(name = LogCollectingTestResource.INCLUDE, value = "io\\.quarkus\\.devservices\\.db2\\..*")
})
public class DevServicesDb2LogTest {
    @Test
    public void testLongDatasourceNameWarning() {
        assertThat(LogCollectingTestResource.current().getRecords())
                .as("Dev Services DB2 warning about 8 character limit")
                .extracting(LogCollectingTestResource::format)
                .anyMatch(log -> log.contains("8 character limit"));
    }
}

package io.quarkus.it.panache.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.LogCollectingTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Test if PanacheQuery is using unnecessary SQL for limiting the number of output rows, the log output is tested for the
 * presence of <code>offset</code> or <code>limit</code> in the SQL.
 */
@QuarkusTest
@QuarkusTestResource(value = LogCollectingTestResource.class, restrictToAnnotatedClass = true, initArgs = {
        @ResourceArg(name = LogCollectingTestResource.LEVEL, value = "DEBUG"),
        @ResourceArg(name = LogCollectingTestResource.INCLUDE, value = "org\\.hibernate\\.SQL"),
})
public class NoPagingTest {

    @Test
    @RunOnVertxContext
    public void test(UniAsserter asserter) {
        asserter.assertThat(() -> Panache.withTransaction(() -> PageItem.findAll().list()), ignored -> {
            assertThat(LogCollectingTestResource.current().getRecords())
                    .as("SQL logs")
                    .extracting(LogCollectingTestResource::format)
                    .anySatisfy(message -> assertThat(message)
                            .as("Hibernate query must be logged")
                            .matches(".*select .* from PageItem .*"))
                    .noneSatisfy(message -> assertThat(message)
                            .as("PanacheQuery must not generate SQL with limits when no paging has been requested")
                            .matches(".*select .* limit .*"))
                    .noneSatisfy(message -> assertThat(message)
                            .as("PanacheQuery must not generate SQL with offsets when no paging has been requested")
                            .matches(".*select .* offset .*"));
        });
    }

}

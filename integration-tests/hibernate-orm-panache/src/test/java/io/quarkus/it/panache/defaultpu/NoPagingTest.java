package io.quarkus.it.panache.defaultpu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.LogCollectingTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;

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
    public void test() {
        PageItem.findAll().list();

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
    }

}

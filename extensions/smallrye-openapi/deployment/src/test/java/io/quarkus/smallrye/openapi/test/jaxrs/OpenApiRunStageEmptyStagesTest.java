package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;
import java.util.logging.Formatter;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.openapi.OASFilter;
import org.hamcrest.MatcherAssert;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.test.QuarkusUnitTest;

class OpenApiRunStageEmptyStagesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(EmptyStageArrayFilter.class))
            .setLogRecordPredicate(record -> record.getLoggerName().startsWith("io.quarkus.smallrye.openapi"))
            .assertLogRecords(records -> {
                Formatter formatter = new PatternFormatter("[%p] %m");
                List<String> lines = records.stream().map(formatter::format).map(String::trim).toList();

                MatcherAssert.assertThat(lines, hasItem(is(
                        "[WARN] @OpenApiFilter on 'io.quarkus.smallrye.openapi.test.jaxrs.OpenApiRunStageEmptyStagesTest$EmptyStageArrayFilter' will not be run, since the stages array is set to an empty array (stages = {}).")));
            });

    @Singleton
    @OpenApiFilter(stages = {})
    public static class EmptyStageArrayFilter implements OASFilter {
    }

    @Test
    public void testMultiStageFiltersApplied() throws IOException {
        // empty, see runner assertLogRecords
    }
}

package io.quarkus.it.jpa.h2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.LogCollectingTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that Hibernate ORM does not log any warning on startup with this particular database.
 * <p>
 * In particular, this checks that there are no warnings related to the use of a deprecated dialect
 * or a database version that is not supported by the dialect.
 * <p>
 * Note LogCollectingTestResource cannot be used in native mode,
 * hence the lack of a corresponding native mode test.
 */
@QuarkusTest
@QuarkusTestResource(value = LogCollectingTestResource.class, restrictToAnnotatedClass = true, initArgs = {
        @ResourceArg(name = LogCollectingTestResource.LEVEL, value = "WARNING"),
        @ResourceArg(name = LogCollectingTestResource.INCLUDE, value = "org\\.hibernate\\..*"),
        //We actually have a single warning, caused by the intentional peculiarities of io.quarkus.it.jpa.h2.CompanyCustomer:
        @ResourceArg(name = LogCollectingTestResource.EXCLUDE, value = "org\\.hibernate\\.metamodel\\.internal\\.EntityRepresentationStrategyPojoStandard"),
})
public class HibernateOrmNoWarningsTest {
    @Test
    public void testNoWarningsOnStartup() {
        assertThat(LogCollectingTestResource.current().getRecords())
                // There shouldn't be any warning or error
                .as("Startup logs (warning or higher)")
                .extracting(LogCollectingTestResource::format)
                .isEmpty();
    }
}

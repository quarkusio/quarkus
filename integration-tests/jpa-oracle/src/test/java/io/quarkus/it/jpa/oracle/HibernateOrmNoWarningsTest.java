package io.quarkus.it.jpa.oracle;

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
        // Ignore logs about schema management:
        // they are unfortunate (https://github.com/quarkusio/quarkus/issues/16204)
        // but for now we have to live with them.
        @ResourceArg(name = LogCollectingTestResource.EXCLUDE, value = "org\\.hibernate\\.tool\\.schema.*")
})
public class HibernateOrmNoWarningsTest {
    @Test
    public void testNoWarningsOnStartup() {
        assertThat(LogCollectingTestResource.current().getRecords()
                // Ignore logs about JDBC fetch size: Oracle's default is very wrong.
                // See:
                // https://hibernate.zulipchat.com/#narrow/channel/132094-hibernate-orm-dev/topic/JDBC.20fetch.20size.20warning/with/532321427
                // https://github.com/hibernate/hibernate-orm/pull/10633
                // https://in.relation.to/2025/01/24/jdbc-fetch-size/
                // https://github.com/hibernate/hibernate-orm/pull/10636
                // Also, the Hibernate team is in talks with Oracle to get this fixed, so hopefully this will disappear soon.
                .stream().filter(r -> !r.getMessage().contains("Low default JDBC fetch size")))
                // There shouldn't be any warning or error
                .as("Startup logs (warning or higher)")
                .extracting(LogCollectingTestResource::format)
                .isEmpty();
    }
}

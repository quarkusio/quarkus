package io.quarkus.scheduler.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoNamedDataSourceErrorTest {

    @RegisterExtension
    static final QuarkusUnitTest testNoNamedDatasourceError = new QuarkusUnitTest()
            .setExpectedException(IllegalStateException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleJobs.class)
                    .addAsResource(new StringAsset("simpleJobs.cron=0/1 * * * * ?" +
                            "\nsimpleJobs.every=1s" +
                            "\nquarkus.scheduler.state-store=jdbc\n" +
                            "\nquarkus.scheduler.state-store.datasource.name=ds-name\n"),
                            "application.properties"));

    @Test
    public void shouldFailMissingDataSource() throws InterruptedException {
        /**
         * Should not reach here
         */
        Assertions.fail();
    }

}

package io.quarkus.agroal.test;

import static io.quarkus.agroal.test.MultipleDataSourcesTestUtil.testDataSource;

import java.sql.SQLException;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleDataSourcesErroneousButWorkingConfigTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @DataSource("users")
    AgroalDataSource dataSource1;

    @Inject
    @DataSource("inventory")
    AgroalDataSource dataSource2;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MultipleDataSourcesTestUtil.class))
            .withConfigurationResource("application-multiple-datasources-erroneous-but-working.properties");

    @Test
    public void testDataSourceInjection() throws SQLException {
        testDataSource("default", defaultDataSource, "jdbc:h2:tcp://localhost/mem:default", "username-default", 13);
        testDataSource("users", dataSource1, "jdbc:h2:tcp://localhost/mem:users", "username1", 11);
        testDataSource("inventory", dataSource2, "jdbc:h2:tcp://localhost/mem:inventory", "username2", 12);
    }

}

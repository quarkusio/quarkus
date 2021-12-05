package io.quarkus.agroal.test;

import static io.quarkus.agroal.test.MultipleDataSourcesTestUtil.testDataSource;

import java.sql.SQLException;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleDataSourcesConfigTest {

    //tag::injection[]
    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @DataSource("users")
    AgroalDataSource dataSource1;

    @Inject
    @DataSource("inventory")
    AgroalDataSource dataSource2;
    //end::injection[]

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MultipleDataSourcesTestUtil.class))
            .withConfigurationResource("application-multiple-datasources.properties");

    @Test
    public void testDataSourceInjection() throws SQLException {
        testDataSource("default", defaultDataSource, "jdbc:h2:tcp://localhost/mem:default", "username-default", 13);
        testDataSource("users", dataSource1, "jdbc:h2:tcp://localhost/mem:users", "username1", 11);
        testDataSource("inventory", dataSource2, "jdbc:h2:tcp://localhost/mem:inventory", "username2", 12);
    }
}

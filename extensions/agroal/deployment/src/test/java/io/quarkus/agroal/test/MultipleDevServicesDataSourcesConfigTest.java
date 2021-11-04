package io.quarkus.agroal.test;

import static io.quarkus.agroal.test.MultipleDataSourcesTestUtil.testDataSource;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleDevServicesDataSourcesConfigTest {

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
            .withConfigurationResource("application-multiple-devservices-datasources.properties");

    @Test
    public void testDataSourceInjection() throws SQLException {
        testDataSource("default", defaultDataSource,
                "jdbc:h2:tcp://localhost:" + extractPort(defaultDataSource) + "/mem:default;DB_CLOSE_DELAY=-1", "sa", 20);
        testDataSource("users", dataSource1,
                "jdbc:h2:tcp://localhost:" + extractPort(dataSource1) + "/mem:users;DB_CLOSE_DELAY=-1", "sa", 20);
        testDataSource("inventory", dataSource2,
                "jdbc:h2:tcp://localhost:" + extractPort(dataSource2) + "/mem:inventory;DB_CLOSE_DELAY=-1", "sa", 20);
    }

    public int extractPort(AgroalDataSource ds) {
        String url = ds.getConfiguration().connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl();
        Matcher matcher = Pattern.compile("jdbc:h2:tcp://localhost:(\\d+)/").matcher(url);
        matcher.find();
        return Integer.parseInt(matcher.group(1));
    }
}

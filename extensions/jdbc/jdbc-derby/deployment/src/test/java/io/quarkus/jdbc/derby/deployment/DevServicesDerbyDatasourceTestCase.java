package io.quarkus.jdbc.derby.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesDerbyDatasourceTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class);
                }
            });

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testDatasource() throws Exception {
        AgroalConnectionPoolConfiguration configuration = null;

        try {
            configuration = dataSource.getConfiguration().connectionPoolConfiguration();
        } catch (NullPointerException e) {
            // we catch the NPE here as we have a proxycd  and we can't test dataSource directly
            fail("Datasource should not be null");
        }
        assertTrue(configuration.connectionFactoryConfiguration().jdbcUrl().contains("jdbc:derby:"));
        assertEquals(20, configuration.maxSize());

        try (Connection connection = dataSource.getConnection()) {
        }
    }
}

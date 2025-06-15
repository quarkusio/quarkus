package io.quarkus.hibernate.search.orm.elasticsearch.test.configuration;

import java.sql.SQLException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoConfigNoIndexedEntityNamedPuTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .withConfigurationResource("application-nohsearchconfig-named-pu.properties");

    @Test
    public void testNoConfig() throws SQLException {
        // we should be able to start the application, even with no configuration at all nor indexed entities
    }
}

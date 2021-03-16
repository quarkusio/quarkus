package io.quarkus.hibernate.search.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.test.QuarkusUnitTest;

public class NoConfigIndexedEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class)
                    .addAsResource("application-nohsearchconfig.properties", "application.properties"))
            .assertException(throwable -> assertThat(throwable)
                    .isInstanceOf(ConfigurationError.class)
                    .hasMessageContaining("The Elasticsearch version needs to be defined via properties:"
                            + " quarkus.hibernate-search-orm.elasticsearch.version"));

    @Test
    public void testNoConfig() throws SQLException {
        // an exception should be thrown
    }
}

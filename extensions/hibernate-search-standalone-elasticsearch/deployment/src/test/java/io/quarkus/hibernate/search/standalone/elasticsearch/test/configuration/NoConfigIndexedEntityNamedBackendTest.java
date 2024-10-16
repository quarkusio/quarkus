package io.quarkus.hibernate.search.standalone.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class NoConfigIndexedEntityNamedBackendTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntityInNamedBackend.class))
            .assertException(throwable -> assertThat(throwable)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("The Elasticsearch version needs to be defined via properties:"
                            + " quarkus.hibernate-search-standalone.elasticsearch.\"mybackend\".version"));

    @Test
    public void testNoConfig() throws SQLException {
        // an exception should be thrown
    }
}

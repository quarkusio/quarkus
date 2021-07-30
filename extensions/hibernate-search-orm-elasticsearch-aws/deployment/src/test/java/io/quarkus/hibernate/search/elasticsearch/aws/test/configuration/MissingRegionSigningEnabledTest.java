package io.quarkus.hibernate.search.elasticsearch.aws.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MissingRegionSigningEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class)
                    .addAsResource("application-missing-region-signing-enabled.properties", "application.properties"))
            .assertException(throwable -> assertThat(throwable)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContainingAll(
                            "When AWS request signing is enabled, the AWS region needs to be defined"
                                    + " via property 'quarkus.hibernate-search-orm.elasticsearch.aws.region'."));

    @Test
    public void testNoConfig() {
        Assertions.fail("an exception should be thrown when deploying");
    }
}

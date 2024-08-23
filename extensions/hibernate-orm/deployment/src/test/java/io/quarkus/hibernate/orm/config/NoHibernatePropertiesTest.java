package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Since we decided that we're no longer supporting to read an hibernate.properties resource,
 * let's also test that this is made explicit.
 * N.B. while we no longer parse the file during boot, there are other components in Hibernate ORM
 * that look for it so this would lead to inconsistencies.
 */
public class NoHibernatePropertiesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .assertException(t -> {
                assertThat(t)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContainingAll(
                                "The Hibernate ORM configuration in Quarkus does not support sourcing configuration properties from resources named `hibernate.properties`");
            })
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource(new StringAsset(""), "hibernate.properties"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

}

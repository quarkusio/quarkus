package io.quarkus.hibernate.orm.xml.persistence;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class PersistenceXmlAndQuarkusConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().assertException(
            e -> Assertions.assertThat(e).isInstanceOf(ConfigurationException.class).hasMessageContainingAll(
                    "A legacy persistence.xml file is present in the classpath, but Hibernate ORM is also configured through the Quarkus config file",
                    "Legacy persistence.xml files and Quarkus configuration cannot be used at the same time",
                    "To ignore persistence.xml files, set the configuration property 'quarkus.hibernate-orm.persistence-xml.ignore' to 'true'",
                    "To use persistence.xml files, remove all 'quarkus.hibernate-orm.*' properties from the Quarkus config file."))
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(MyEntity.class)
                    .addAsManifestResource("META-INF/some-persistence.xml", "persistence.xml"))
            .withConfigurationResource("application.properties")
            // Unfortunately the minimal config is not enough to detect that a PU is configured in Quarkus's
            // application.properties
            // -- we're paying the price of our "zero config" approach.
            // We can only detect something is wrong if some optional properties are set.
            .overrideConfigKey("quarkus.hibernate-orm.fetch.max-depth", "2");

    @Test
    public void test() {
        // should not be called, deployment exception should happen first:
        // it's illegal to have Hibernate configuration properties in both the
        // application.properties and in the persistence.xml
        Assertions.fail("Application should not start");
    }

}

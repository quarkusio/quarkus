package io.quarkus.hibernate.orm.xml.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.PERSISTENCE_UNIT_NAME;

import java.util.logging.Formatter;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class PersistenceXmlTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.INFO))
            .assertLogRecords(records -> assertThat(records).as("Logs on startup").anySatisfy(record -> {
                assertThat(LOG_FORMATTER.formatMessage(record)).contains(
                        "A legacy persistence.xml file is present in the classpath",
                        "any configuration of the Hibernate ORM extension will be ignored",
                        "To ignore persistence.xml files",
                        "set the configuration property 'quarkus.hibernate-orm.persistence-xml.ignore' to 'true'");
            }))
            .withApplicationRoot((jar) -> jar.addClass(SmokeTestUtils.class).addClass(MyEntity.class)
                    .addAsManifestResource("META-INF/some-persistence.xml", "persistence.xml")
                    .addAsResource("application-datasource-only.properties", "application.properties"));

    @Inject
    EntityManager entityManager;

    @Test
    public void puIsFromPersistenceXml() {
        Arc.container().requestContext().activate();
        try {
            // the PU is templatePU from the persistence.xml, not the default entity manager from application.properties
            Assertions.assertEquals("templatePU",
                    entityManager.getEntityManagerFactory().getProperties().get(PERSISTENCE_UNIT_NAME));
        } finally {
            Arc.container().requestContext().deactivate();
        }
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(entityManager, MyEntity.class, MyEntity::new, e -> e.id,
                (e, value) -> e.name = value, e -> e.name);
    }

}

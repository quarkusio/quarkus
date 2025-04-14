package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.test.QuarkusUnitTest;

public class DbVersionExtraSpaceTest {

    private static final String ACTUAL_H2_VERSION = DialectVersions.Defaults.H2;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            // IMPORTANT: we insert spaces here -- both before and after, as this seems to trigger different behavior.
            // See https://github.com/quarkusio/quarkus/issues/39395
            .overrideConfigKey("quarkus.datasource.db-version", " " + ACTUAL_H2_VERSION + " ")
            // Expect no warnings (in particular from Hibernate ORM)
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    // Ignore these particular warnings: they are not relevant to this test.
                    && !record.getMessage().contains("has been blocked for") //sometimes CI has a super slow moment and this triggers the blocked thread detector
                    && !record.getMessage().contains("Agroal")
                    && !record.getMessage().contains("Netty DefaultChannelId initialization"))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage) // This is just to get meaningful error messages, as LogRecord doesn't have a toString()
                    .isEmpty());

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    @Test
    public void dialectVersion() {
        var dialectVersion = sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect().getVersion();
        assertThat(DialectVersions.toString(dialectVersion)).isEqualTo(ACTUAL_H2_VERSION);
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(session,
                MyEntity.class, MyEntity::new,
                MyEntity::getId,
                MyEntity::setName, MyEntity::getName);
    }
}

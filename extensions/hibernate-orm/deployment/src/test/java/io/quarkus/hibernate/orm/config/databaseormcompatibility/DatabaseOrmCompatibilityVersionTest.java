package io.quarkus.hibernate.orm.config.databaseormcompatibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.AvailableSettings;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.config.SettingsSpyingIdentifierGenerator;
import io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider;
import io.quarkus.test.QuarkusUnitTest;

public class DatabaseOrmCompatibilityVersionTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SpyingIdentifierGeneratorEntity.class)
                    .addClass(SettingsSpyingIdentifierGenerator.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.database.orm-compatibility.version", "5.6")
            // We allow overriding database/orm compatibility settings with .unsupported-properties,
            // to enable step-by-step migration
            .overrideConfigKey(
                    "quarkus.hibernate-orm.unsupported-properties.\"" + AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE + "\"",
                    "TIMESTAMP_UTC")
            // Expect warnings on startup
            .setLogRecordPredicate(record -> FastBootHibernatePersistenceProvider.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> {
                var assertion = assertThat(records)
                        .as("Warnings on startup")
                        .hasSizeGreaterThanOrEqualTo(3);
                assertion.element(0).satisfies(record -> assertThat(LOG_FORMATTER.formatMessage(record))
                        .contains("Persistence-unit [<default>] sets unsupported properties")
                        // We should not log property values, that could be a security breach for some properties.
                        .doesNotContain("some-value"));
                assertion.element(1).satisfies(record -> assertThat(LOG_FORMATTER.formatMessage(record))
                        .contains("Persistence-unit [<default>]:"
                                + " enabling best-effort backwards compatibility with 'quarkus.hibernate-orm.database.orm-compatibility.version=5.6'.",
                                "Quarkus will attempt to change the behavior and expected schema of Hibernate ORM"
                                        + " to match those of Hibernate ORM 5.6.",
                                "This is an inherently best-effort feature",
                                "may stop working in future versions of Quarkus",
                                "Consider migrating your application",
                                "https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0:-Hibernate-ORM-5-to-6-migration"));
                assertion.anySatisfy(record -> assertThat(LOG_FORMATTER.formatMessage(record))
                        .contains(
                                "Persistence-unit [<default>] - 5.6 compatibility: setting 'hibernate.timezone.default_storage=NORMALIZE'.",
                                "affects Hibernate ORM's behavior and schema compatibility",
                                "may stop working in future versions of Quarkus"));
            });

    @Inject
    EntityManagerFactory emf;

    @Inject
    EntityManager em;

    @Test
    public void testPropertiesPropagatedToStaticInit() {
        // Two sets of settings: 0 is static init, 1 is runtime init.
        assertThat(SettingsSpyingIdentifierGenerator.collectedSettings).hasSize(2);
        Map<String, Object> settings = SettingsSpyingIdentifierGenerator.collectedSettings.get(0);
        assertThat(settings).containsAllEntriesOf(Map.of(
                AvailableSettings.TIMEZONE_DEFAULT_STORAGE, "NORMALIZE",
                // We allow overriding database/orm compatibility settings with .unsupported-properties,
                // to enable step-by-step migration
                AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE, "TIMESTAMP_UTC"));
    }

    @Test
    public void testPropertiesPropagatedToRuntimeInit() {
        assertThat(emf.getProperties()).containsAllEntriesOf(Map.of(
                AvailableSettings.TIMEZONE_DEFAULT_STORAGE, "NORMALIZE",
                // We allow overriding database/orm compatibility settings with .unsupported-properties,
                // to enable step-by-step migration
                AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE, "TIMESTAMP_UTC"));
    }

    @Entity
    public static class SpyingIdentifierGeneratorEntity {
        @Id
        @GeneratedValue(generator = "spying-generator")
        @GenericGenerator(name = "spying-generator", strategy = "io.quarkus.hibernate.orm.config.SettingsSpyingIdentifierGenerator")
        private Long id;

        public SpyingIdentifierGeneratorEntity() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}

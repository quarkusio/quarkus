package io.quarkus.hibernate.reactive.config.unsupportedproperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.config.SettingsSpyingIdentifierGenerator;
import io.quarkus.hibernate.reactive.runtime.FastBootHibernateReactivePersistenceProvider;
import io.quarkus.test.QuarkusUnitTest;

public class UnsupportedPropertiesTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SpyingIdentifierGeneratorEntity.class)
                    .addClass(SettingsSpyingIdentifierGenerator.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.jdbc.statement-batch-size", "10")
            // This should be taken into account by Hibernate ORM
            .overrideConfigKey("quarkus.hibernate-orm.unsupported-properties.\"" + AvailableSettings.ORDER_INSERTS + "\"",
                    "true")
            // This is just to test a property set at build time
            .overrideConfigKey("quarkus.hibernate-orm.unsupported-properties.\"hibernate.some.unknown.key.static-and-runtime\"",
                    "some-value-1")
            // This is just to test a property set at runtime, which would not be available during the build
            // (or even during static init with native applications).
            .overrideRuntimeConfigKey(
                    "quarkus.hibernate-orm.unsupported-properties.\"hibernate.some.unknown.key.runtime-only\"",
                    "some-value-2")
            // This should be ignored with a warning
            .overrideConfigKey(
                    "quarkus.hibernate-orm.unsupported-properties.\"" + AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION
                            + "\"",
                    "drop-and-create")
            // Expect warnings on startup
            .setLogRecordPredicate(
                    record -> FastBootHibernateReactivePersistenceProvider.class.getName().equals(record.getLoggerName())
                            && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> {
                var assertion = assertThat(records)
                        .as("Warnings on startup")
                        .hasSize(2);
                assertion.element(0).satisfies(record -> assertThat(LOG_FORMATTER.formatMessage(record))
                        .contains("Persistence-unit [default-reactive] sets unsupported properties",
                                "These properties may not work correctly",
                                "may change when upgrading to a newer version of Quarkus (even just a micro/patch version)",
                                "Consider using a supported configuration property",
                                "make sure to file a feature request so that a supported configuration property can be added to Quarkus")
                        .contains(AvailableSettings.ORDER_INSERTS, AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION,
                                "hibernate.some.unknown.key.static-and-runtime", "hibernate.some.unknown.key.runtime-only")
                        // We should not log property values, that could be a security breach for some properties.
                        .doesNotContain("some-value"));
                assertion.element(1).satisfies(record -> assertThat(LOG_FORMATTER.formatMessage(record))
                        .contains(
                                "Persistence-unit [default-reactive] sets property '"
                                        + AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION
                                        + "' to a custom value through 'quarkus.hibernate-orm.unsupported-properties.\""
                                        + AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION + "\"'",
                                "Quarkus already set that property independently",
                                "The custom value will be ignored"));
            });

    @Inject
    SessionFactory ormSessionFactory; // This is an ORM SessionFactory, but it's backing Hibernate Reactive.

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    public void testPropertiesPropagatedToStaticInit() {
        assertThat(SettingsSpyingIdentifierGenerator.collectedSettings).hasSize(1);
        Map<String, Object> settings = SettingsSpyingIdentifierGenerator.collectedSettings.get(0);
        assertThat(settings)
                .containsEntry("hibernate.some.unknown.key.static-and-runtime", "some-value-1")
                .doesNotContainKey("hibernate.some.unknown.key.runtime-only");
    }

    @Test
    public void testPropertiesPropagatedToRuntimeInit() {
        assertThat(ormSessionFactory.getProperties())
                .contains(entry("hibernate.order_inserts", "true"),
                        // Also test a property that Quarkus cannot possibly know about
                        entry("hibernate.some.unknown.key.static-and-runtime", "some-value-1"),
                        entry("hibernate.some.unknown.key.runtime-only", "some-value-2"));
    }

    @Entity
    public static class SpyingIdentifierGeneratorEntity {
        @Id
        @GeneratedValue(generator = "spying-generator")
        @GenericGenerator(name = "spying-generator", strategy = "io.quarkus.hibernate.reactive.config.SettingsSpyingIdentifierGenerator")
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

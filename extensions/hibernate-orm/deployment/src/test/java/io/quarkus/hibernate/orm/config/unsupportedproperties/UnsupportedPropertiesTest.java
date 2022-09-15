package io.quarkus.hibernate.orm.config.unsupportedproperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.AvailableSettings;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;

public class UnsupportedPropertiesTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(ParentEntity.class)
                    .addClass(ChildEntity.class)
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
            // This is just to test a property set at runtime, which which would not be available during the build
            // (or even during static init with native applications).
            .overrideRuntimeConfigKey(
                    "quarkus.hibernate-orm.unsupported-properties.\"hibernate.some.unknown.key.runtime-only\"",
                    "some-value-2")
            // This should be ignored with a warning
            .overrideConfigKey(
                    "quarkus.hibernate-orm.unsupported-properties.\"" + AvailableSettings.HBM2DDL_DATABASE_ACTION + "\"",
                    "drop-and-create")
            // Expect warnings on startup
            .setLogRecordPredicate(record -> FastBootHibernatePersistenceProvider.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> {
                var assertion = assertThat(records)
                        .as("Warnings on startup")
                        .hasSize(2);
                assertion.element(0).satisfies(record -> assertThat(LOG_FORMATTER.formatMessage(record))
                        .contains("Persistence-unit [<default>] sets unsupported properties",
                                "These properties may not work correctly",
                                "may change when upgrading to a newer version of Quarkus (even just a micro/patch version)",
                                "Consider using a supported configuration property",
                                "make sure to file a feature request so that a supported configuration property can be added to Quarkus")
                        .contains(AvailableSettings.ORDER_INSERTS, AvailableSettings.HBM2DDL_DATABASE_ACTION,
                                "hibernate.some.unknown.key.static-and-runtime", "hibernate.some.unknown.key.runtime-only")
                        // We should not log property values, that could be a security breach for some properties.
                        .doesNotContain("some-value"));
                assertion.element(1).satisfies(record -> assertThat(LOG_FORMATTER.formatMessage(record))
                        .contains(
                                "Persistence-unit [<default>] sets property '" + AvailableSettings.HBM2DDL_DATABASE_ACTION
                                        + "' to a custom value through 'quarkus.hibernate-orm.unsupported-properties.\""
                                        + AvailableSettings.HBM2DDL_DATABASE_ACTION + "\"'",
                                "Quarkus already set that property independently",
                                "The custom value will be ignored"));
            });

    @Inject
    EntityManagerFactory emf;

    @Inject
    EntityManager em;

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
        assertThat(emf.getProperties())
                .contains(entry("hibernate.order_inserts", "true"),
                        // Also test a property that Quarkus cannot possibly know about
                        entry("hibernate.some.unknown.key.static-and-runtime", "some-value-1"),
                        entry("hibernate.some.unknown.key.runtime-only", "some-value-2"));
    }

    // This tests a particular feature that can (currently) only be enabled with unsupported properties
    @Test
    public void testInsertsOrdered() {
        var listener = new BatchCountSpyingEventListener();

        QuarkusTransaction.run(() -> {
            em.unwrap(Session.class).addEventListeners(listener);

            ParentEntity parent1 = new ParentEntity(1);
            parent1.getChildren().add(new ChildEntity(2));
            parent1.getChildren().add(new ChildEntity(3));
            ParentEntity parent2 = new ParentEntity(4);
            parent2.getChildren().add(new ChildEntity(5));
            parent2.getChildren().add(new ChildEntity(6));
            em.persist(parent1);
            em.persist(parent2);
        });

        // Thanks to insert ordering, we expect only 2 JDBC statement batches
        // (instead of 4 if insert ordering was disabled)
        assertThat(listener.batchCount).isEqualTo(2);
    }

    private static class BatchCountSpyingEventListener extends BaseSessionEventListener {
        private long batchCount = 0;

        @Override
        public void jdbcExecuteBatchStart() {
            ++batchCount;
        }
    }

    @Entity
    public static class ParentEntity {
        @Id
        private Long id;

        @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
        private List<ChildEntity> children = new ArrayList<>();

        public ParentEntity() {
        }

        public ParentEntity(long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public List<ChildEntity> getChildren() {
            return children;
        }

        public void setChildren(List<ChildEntity> children) {
            this.children = children;
        }
    }

    @Entity
    public static class ChildEntity {
        @Id
        private Long id;

        @ManyToOne
        private ParentEntity parent;

        public ChildEntity() {
        }

        public ChildEntity(long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public ParentEntity getParent() {
            return parent;
        }

        public void setParent(ParentEntity parent) {
            this.parent = parent;
        }
    }

    @Entity
    public static class SpyingIdentifierGeneratorEntity {
        @Id
        @GeneratedValue(generator = "spying-generator")
        @GenericGenerator(name = "spying-generator", strategy = "io.quarkus.hibernate.orm.config.unsupportedproperties.SettingsSpyingIdentifierGenerator")
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

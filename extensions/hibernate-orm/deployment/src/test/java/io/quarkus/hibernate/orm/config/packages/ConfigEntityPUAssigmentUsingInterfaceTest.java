package io.quarkus.hibernate.orm.config.packages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.logging.Formatter;
import java.util.logging.Level;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.config.differentpackage.INamedEntity;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/22183
 */
public class ConfigEntityPUAssigmentUsingInterfaceTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EntityImplementingInterface.class, INamedEntity.class))
            // In a real-world scenario, this would be used only if there are multiple PUs,
            // but this is simpler and enough to reproduce the issue.
            .overrideConfigKey("quarkus.hibernate-orm.packages", EntityImplementingInterface.class.getPackageName())
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            // We don't expect any warning, in particular not:
            // "Could not find a suitable persistence unit for model classes:"
            .assertLogRecords(records -> assertThat(records).extracting(LOG_FORMATTER::format).isEmpty());

    @Inject
    Session session;

    @Test
    @ActivateRequestContext
    void smoke() {
        // We just want to check the lack of warnings... but let's at least check the entity works correctly.
        assertThatCode(() -> session.createSelectionQuery("select count(*) from EntityImplementingInterface", Long.class))
                .doesNotThrowAnyException();
    }

    @Entity(name = "EntityImplementingInterface")
    public static class EntityImplementingInterface implements INamedEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

package io.quarkus.hibernate.orm.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Formatter;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class LogBindParametersTrueTest {
    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.log.bind-parameters", "true")
            // Expect a trace
            .setLogRecordPredicate(record -> record.getMessage().contains("binding parameter"))
            .assertLogRecords(records -> assertThat(records)
                    .hasSize(2)
                    .anySatisfy(record -> {
                        assertThat(record.getLevel().getName()).isEqualTo("TRACE");
                        assertThat(LOG_FORMATTER.formatMessage(record))
                                .contains("SomeName");
                    }));

    @Inject
    EntityManager em;

    @Test
    @Transactional
    public void testFormattedValue() {
        em.persist(new MyEntity("SomeName"));
    }

}

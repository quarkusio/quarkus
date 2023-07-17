package io.quarkus.hibernate.orm.log;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class LogBindParametersDefaultValueTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class))
            .withConfigurationResource("application.properties")
            // Expect no trace
            .setLogRecordPredicate(record -> record.getMessage().contains("binding parameter"))
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @Inject
    EntityManager em;

    @Test
    @Transactional
    public void testFormattedValue() {
        em.persist(new MyEntity("SomeName"));
    }

}

package io.quarkus.hibernate.orm.panache.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import jakarta.persistence.Entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/33832
 */
public class PreExistingGetterTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(PreExistingGetterEntity.class));

    @Test
    void testDeserialization() throws IOException {
        var objectMapper = new ObjectMapper()
                // This module is necessary to reproduce the bug.
                .registerModule(new JakartaXmlBindAnnotationModule());
        PreExistingGetterEntity read = objectMapper.reader().readValue("{\"field\": \"foo\"}",
                PreExistingGetterEntity.class);
        assertThat(read).isNotNull();
        assertThat(read.getField()).isEqualTo("foo");
    }

    @Entity
    public static class PreExistingGetterEntity extends PanacheEntity {
        public String field;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }
}

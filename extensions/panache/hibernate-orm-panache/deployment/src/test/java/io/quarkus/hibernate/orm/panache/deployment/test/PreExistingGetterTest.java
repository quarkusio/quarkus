package io.quarkus.hibernate.orm.panache.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/33832
 */
public class PreExistingGetterTest {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PreExistingGetterEntity.class));

    @Test
    void testDeserialization() {
        var objectMapper = JsonMapper.builder()
                // This module is necessary to reproduce the bug.
                .addModule(new JakartaXmlBindAnnotationModule())
                .build();
        PreExistingGetterEntity read = objectMapper.reader()
                .forType(PreExistingGetterEntity.class).readValue("{\"field\": \"foo\"}");
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

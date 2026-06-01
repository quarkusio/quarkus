package io.quarkus.hibernate.panache.deployment.test;

import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;

import org.hibernate.id.IdentifierGenerationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.WithId;
import io.quarkus.test.QuarkusExtensionTest;

public class WithIdTest {

    @Entity
    public static class CustomIdEntity extends WithId<Long> implements PanacheEntity.Managed {
        public String name;
    }

    @Entity
    public static class AutoLongEntity extends WithId.AutoLong implements PanacheEntity.Managed {
        public String name;
    }

    @Entity
    public static class AutoStringEntity extends WithId.AutoString implements PanacheEntity.Managed {
        public String name;
    }

    @Entity
    public static class AutoUUIDEntity extends WithId.AutoUUID implements PanacheEntity.Managed {
        public String name;
    }

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(CustomIdEntity.class, AutoLongEntity.class, AutoStringEntity.class, AutoUUIDEntity.class));

    @Transactional
    @Test
    void testCustomId() {
        CustomIdEntity entity = new CustomIdEntity();
        entity.name = "custom";
        // should throw
        Assertions.assertThrows(IdentifierGenerationException.class, () -> entity.persist());
        Assertions.assertNull(entity.id);
        entity.id = 1l;
        entity.persist();
        Assertions.assertEquals("CustomIdEntity<" + entity.id + ">", entity.toString());
    }

    @Transactional
    @Test
    void testAutoLong() {
        AutoLongEntity entity = new AutoLongEntity();
        entity.name = "long";
        entity.persist();
        Assertions.assertNotNull(entity.id);
        Assertions.assertEquals("AutoLongEntity<" + entity.id + ">", entity.toString());
    }

    @Transactional
    @Test
    void testAutoString() {
        AutoStringEntity entity = new AutoStringEntity();
        entity.name = "string";
        entity.persist();
        Assertions.assertNotNull(entity.id);
        Assertions.assertFalse(entity.id.isEmpty());
        Assertions.assertEquals("AutoStringEntity<" + entity.id + ">", entity.toString());
    }

    @Transactional
    @Test
    void testAutoUUID() {
        AutoUUIDEntity entity = new AutoUUIDEntity();
        entity.name = "uuid";
        entity.persist();
        Assertions.assertNotNull(entity.id);
        Assertions.assertEquals("AutoUUIDEntity<" + entity.id + ">", entity.toString());
    }
}

package io.quarkus.hibernate.orm.attributeconverter;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.AttributeConverter;
import javax.persistence.Basic;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.transaction.UserTransaction;

import org.hibernate.Session;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that {@code @Converter(autoApply = true)} works correctly.
 * <p>
 * This test used to fail on startup when the annotation was not detected correctly.
 */
public class AttributeConverterAutoApplyTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyEntity.class)
                    .addClass(MyData.class)
                    .addClass(MyDataConverter.class))
            .withConfigurationResource("application.properties");

    @Inject
    Session session;

    @Inject
    UserTransaction transaction;

    @Test
    @SuppressWarnings("unchecked")
    public void testConverter() throws Exception {
        transaction.begin();
        MyEntity entity = new MyEntity();
        entity.myData = new MyData("foo");
        session.persist(entity);
        transaction.commit();

        transaction.begin();
        assertThat(session.createNativeQuery("select myData from myentity").getResultList())
                .containsExactly("foo");
        transaction.commit();
    }

    @Entity
    @Table(name = "myentity")
    public static class MyEntity {

        @Id
        @GeneratedValue
        public long id;

        @Basic
        public MyData myData;

    }

    public static class MyData {
        public final String value;

        public MyData(String value) {
            this.value = value;
        }
    }

    @Converter(autoApply = true)
    public static class MyDataConverter implements AttributeConverter<MyData, String> {
        @Override
        public String convertToDatabaseColumn(MyData attribute) {
            return attribute == null ? null : attribute.value;
        }

        @Override
        public MyData convertToEntityAttribute(String dbData) {
            return dbData == null ? null : new MyData(dbData);
        }
    }
}

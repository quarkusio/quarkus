package io.quarkus.hibernate.reactive.publicfields;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Checks that public field access is correctly replaced with getter/setter calls,
 * regardless of where the field is declared in the class hierarchy.
 */
public class PublicFieldAccessInheritanceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyMappedSuperclass.class)
                    .addClass(MyAbstractEntity.class)
                    .addClass(MyConcreteEntity.class)
                    .addClass(FieldAccessEnhancedDelegate.class))
            .withConfigurationResource("application.properties");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void testFieldAccess(final UniAsserter asserter) {
        // Ideally we'd write a @ParameterizedTest and pass the delegates as parameters,
        // but we cannot do that due to JUnit using a different classloader than the test.
        for (FieldAccessEnhancedDelegate delegate : FieldAccessEnhancedDelegate
                .values()) {
            doTestFieldAccess(delegate, asserter);
        }
    }

    private void doTestFieldAccess(final FieldAccessEnhancedDelegate delegate, final UniAsserter asserter) {
        //First verify we don't pass the assertion when not modifying the entity:
        asserter.assertThat(() -> sessionFactory.withTransaction(session -> {
            MyConcreteEntity entity = new MyConcreteEntity();
            return session.persist(entity).replaceWith(() -> entity.id);
        }).chain(id -> sessionFactory.withTransaction((session, tx) -> session.find(MyConcreteEntity.class, id))),
                loadedEntity -> notPassingAssertion(loadedEntity, delegate));

        // Now again, but modify the entity and assert dirtiness was detected:
        asserter.assertThat(() -> sessionFactory.withTransaction(session -> {
            MyConcreteEntity entity = new MyConcreteEntity();
            return session.persist(entity).replaceWith(() -> entity.id);
        })
                .chain(id -> sessionFactory.withTransaction(session -> session.find(MyConcreteEntity.class, id)
                        .invoke(delegate::setValue).replaceWith(id)))
                .chain(id -> sessionFactory.withTransaction(session -> session.find(MyConcreteEntity.class, id))),
                delegate::assertValue);
    }

    // Self-test: initially the assertion doesn't pass: the value was not set yet.
    // Verify that we would fail the test in such case.
    private void notPassingAssertion(final MyConcreteEntity entity, final FieldAccessEnhancedDelegate delegate) {
        AssertionError expected = null;
        try {
            delegate.assertValue(entity);
        } catch (AssertionError e) {
            expected = e;
        }
        if (expected == null) {
            throw new IllegalStateException("This test is buggy: assertions should not pass at this point.");
        }
    }

    @MappedSuperclass
    public static class MyMappedSuperclass {

        public Long mappedSuperclassField;

    }

    @Entity(name = "abstract")
    public static abstract class MyAbstractEntity extends MyMappedSuperclass {

        @Id
        @GeneratedValue
        public long id;

        public Long abstractEntityField;

    }

    @Entity(name = "concrete")
    public static class MyConcreteEntity extends MyAbstractEntity {

        public Long concreteEntityField;

    }

    private enum FieldAccessEnhancedDelegate {

        MAPPED_SUPERCLASS {
            @Override
            public void setValue(MyConcreteEntity entity) {
                entity.mappedSuperclassField = 42L;
            }

            @Override
            public void assertValue(MyConcreteEntity entity) {
                assertThat(entity.mappedSuperclassField).isEqualTo(42L);
            }
        },
        ABSTRACT_ENTITY {
            @Override
            public void setValue(MyConcreteEntity entity) {
                entity.abstractEntityField = 42L;
            }

            @Override
            public void assertValue(MyConcreteEntity entity) {
                assertThat(entity.abstractEntityField).isEqualTo(42L);
            }
        },
        CONCRETE_ENTITY {
            @Override
            public void setValue(MyConcreteEntity entity) {
                entity.concreteEntityField = 42L;
            }

            @Override
            public void assertValue(MyConcreteEntity entity) {
                assertThat(entity.concreteEntityField).isEqualTo(42L);
            }
        };

        public abstract void setValue(MyConcreteEntity entity);

        public abstract void assertValue(MyConcreteEntity entity);

    }
}

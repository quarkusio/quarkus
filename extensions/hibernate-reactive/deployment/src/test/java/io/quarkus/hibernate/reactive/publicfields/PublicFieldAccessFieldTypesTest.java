package io.quarkus.hibernate.reactive.publicfields;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Checks that public field access is correctly replaced with getter/setter calls,
 * regardless of the field type.
 */
public class PublicFieldAccessFieldTypesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addClass(FieldAccessEnhancedDelegate.class))
            .withConfigurationResource("application.properties");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void testFieldAccess(UniAsserter asserter) {
        // Ideally we'd write a @ParameterizedTest and pass the delegates as parameters,
        // but we cannot do that due to JUnit using a different classloader than the test.
        for (FieldAccessEnhancedDelegate delegate : FieldAccessEnhancedDelegate.values()) {
            doTestFieldAccess(delegate, asserter);
        }
    }

    private void doTestFieldAccess(final FieldAccessEnhancedDelegate delegate, final UniAsserter asserter) {
        //First verify we don't pass the assertion when not modifying the entity:
        asserter.assertThat(() -> sessionFactory.withTransaction((session, tx) -> {
            MyEntity entity = new MyEntity();
            return session.persist(entity).replaceWith(() -> entity.id);
        })
                .chain(id -> sessionFactory.withTransaction((session, tx) -> session.find(MyEntity.class, id))),
                loadedEntity -> notPassingAssertion(loadedEntity, delegate));

        // Now again, but modify the entity and assert dirtiness was detected:
        asserter.assertThat(() -> sessionFactory.withTransaction((session, tx) -> {
            MyEntity entity = new MyEntity();
            return session.persist(entity).replaceWith(() -> entity.id);
        })
                .chain(id -> sessionFactory
                        .withTransaction((session, tx) -> session.find(MyEntity.class, id).invoke(delegate::setValue))
                        .replaceWith(id))
                .chain(id -> sessionFactory.withTransaction((session, tx) -> session.find(MyEntity.class, id))),
                delegate::assertValue);
    }

    // Self-test: initially the assertion doesn't pass: the value was not set yet.
    // Verify that we would fail the test in such case.
    private void notPassingAssertion(MyEntity entity, FieldAccessEnhancedDelegate delegate) {
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

    @Entity
    public static class MyEntity {

        @Id
        @GeneratedValue
        public long id;

        public LocalDate object;
        public boolean boolean_;
        public int int_;
        public long long_;
        public float float_;
        public double double_;
        public short short_;
        public char char_ = '\n'; // The Reactive postgresql driver doesn't like the zero char, for some reason
        public byte byte_;

    }

    private enum FieldAccessEnhancedDelegate {

        OBJECT {
            @Override
            public void setValue(MyEntity entity) {
                entity.object = LocalDate.of(2017, 11, 7);
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.object).isEqualTo(LocalDate.of(2017, 11, 7));
            }
        },
        BOOLEAN {
            @Override
            public void setValue(MyEntity entity) {
                entity.boolean_ = true;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.boolean_).isTrue();
            }
        },
        INTEGER {
            @Override
            public void setValue(MyEntity entity) {
                entity.int_ = 42;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.int_).isEqualTo(42);
            }
        },
        LONG {
            @Override
            public void setValue(MyEntity entity) {
                entity.long_ = 42L;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.long_).isEqualTo(42L);
            }
        },
        FLOAT {
            @Override
            public void setValue(MyEntity entity) {
                entity.float_ = 42f;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.float_).isEqualTo(42f);
            }
        },
        DOUBLE {
            @Override
            public void setValue(MyEntity entity) {
                entity.double_ = 42d;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.double_).isEqualTo(42d);
            }
        },
        SHORT {
            @Override
            public void setValue(MyEntity entity) {
                entity.short_ = (short) 42;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.short_).isEqualTo((short) 42);
            }
        },
        CHAR {
            @Override
            public void setValue(MyEntity entity) {
                entity.char_ = 'a';
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.char_).isEqualTo('a');
            }
        },
        BYTE {
            @Override
            public void setValue(MyEntity entity) {
                entity.byte_ = (byte) 42;
            }

            @Override
            public void assertValue(MyEntity entity) {
                assertThat(entity.byte_).isEqualTo((byte) 42);
            }
        };

        public abstract void setValue(MyEntity entity);

        public abstract void assertValue(MyEntity entity);

    }
}

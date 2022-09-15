package io.quarkus.arc.test.producer.staticProducers;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests static method/field producers
 */
public class StaticMethodProducerTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StaticMethodProducerTest.class, SomeProducer.class,
            MyQualifier.class);

    @Test
    public void testStaticProducers() {
        // method producers
        InstanceHandle<String> stringMethod = Arc.container().instance(String.class);
        Assertions.assertTrue(stringMethod.isAvailable());
        Assertions.assertEquals("foo", stringMethod.get());
        InstanceHandle<Long> longMethod = Arc.container().instance(Long.class);
        Assertions.assertTrue(longMethod.isAvailable());
        Assertions.assertEquals(2L, longMethod.get());
        InstanceHandle<Double> doubleMethod = Arc.container().instance(Double.class);
        Assertions.assertTrue(doubleMethod.isAvailable());
        Assertions.assertEquals(2.1, doubleMethod.get());

        // field producers
        InstanceHandle<String> stringField = Arc.container().instance(String.class, MyQualifier.Literal.INSTANCE);
        Assertions.assertTrue(stringField.isAvailable());
        Assertions.assertEquals("foo", stringField.get());
        InstanceHandle<Long> longField = Arc.container().instance(Long.class, MyQualifier.Literal.INSTANCE);
        Assertions.assertTrue(longField.isAvailable());
        Assertions.assertEquals(2L, longField.get());
        InstanceHandle<Double> doubleField = Arc.container().instance(Double.class, MyQualifier.Literal.INSTANCE);
        Assertions.assertTrue(doubleField.isAvailable());
        Assertions.assertEquals(2.1, doubleField.get());
    }

    @Singleton
    static class SomeProducer {

        static Long LONG = 2l;
        static String STRING = "foo";
        static Double DOUBLE = 2.1;

        @Produces
        private static Long produceLong() {
            return LONG;
        }

        @Produces
        static String produceString() {
            return STRING;
        }

        @Produces
        public static Double produceDouble() {
            return DOUBLE;
        }

        @Produces
        @MyQualifier
        private static Long longField = LONG;

        @Produces
        @MyQualifier
        static String stringField = STRING;

        @Produces
        @MyQualifier
        public static Double doubleField = DOUBLE;
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target(value = { ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
    static @interface MyQualifier {
        public final static class Literal extends AnnotationLiteral<MyQualifier> implements MyQualifier {

            public static final Literal INSTANCE = new Literal();
            private static final long serialVersionUID = 1L;

        }
    }
}

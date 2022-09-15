package io.quarkus.arc.test.autoinject;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AutoFieldInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AutoFieldInjectionTest.class, Client.class, Producer.class));

    @Inject
    Client bean;

    @Test
    public void testInjectionWorks() {
        assertEquals("ok", bean.foo);
        assertEquals(1l, bean.bar);
        assertNull(Client.staticFoo);
        assertNull(bean.baz);
    }

    @Dependent
    static class Client {

        // @Inject should not be added here
        @MyQualifier
        static String staticFoo;

        // @Inject is added automatically
        @MyQualifier
        String foo;

        @MyQualifier
        Long bar;

        // @Inject should not be added here
        @MyQualifier
        final Long baz;

        Client() {
            this.baz = null;
        }

    }

    static class Producer {

        // @Inject should not be added here
        @MyQualifier
        @Produces
        Long producedLong = 1l;

        @MyQualifier
        @Produces
        String produceString() {
            return "ok";
        }

    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface MyQualifier {

    }
}

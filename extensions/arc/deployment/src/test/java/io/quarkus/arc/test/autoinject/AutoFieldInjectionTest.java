package io.quarkus.arc.test.autoinject;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Qualifier;

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
    }

    @Dependent
    static class Client {

        // @Inject is added automatically
        @MyQualifier
        String foo;

        @MyQualifier
        Long bar;

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

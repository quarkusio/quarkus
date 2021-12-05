package io.quarkus.arc.test.autoproduces;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.inject.Qualifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AutoProducerMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AutoProducerMethodTest.class, Client.class, Producers.class));

    @Inject
    Client bean;

    @ActivateRequestContext
    @Test
    public void testProducerWorks() {
        assertEquals("ok", bean.foo);
        assertEquals(13l, bean.longVal);
        assertTrue(bean.strings.isEmpty());
    }

    @Dependent
    static class Client {

        @Inject
        @MyQualifier
        String foo;

        @Inject
        Long longVal;

        @Inject
        List<String> strings;

    }

    static class Producers {

        // @Produces is added automatically
        @MyQualifier
        String produceString() {
            return "ok";
        }

        // @Produces is added automatically
        @Dependent
        static Long produceLong() {
            return 13l;
        }

        @Model
        List<String> strings() {
            return Collections.emptyList();
        }

        // void methods should be ignored
        @MyQualifier
        void ignored() {
        }

    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface MyQualifier {

    }
}

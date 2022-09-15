package io.quarkus.arc.test.autoproduces;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AutoProducerMethodDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AutoProducerMethodDisabledTest.class, Client.class, Producers.class)
                    .addAsResource(new StringAsset("quarkus.arc.auto-producer-methods=false"), "application.properties"));

    @Inject
    Client bean;

    @ActivateRequestContext
    @Test
    public void testProducerIsIgnored() {
        assertFalse(bean.foo.isResolvable());
    }

    @Dependent
    static class Client {

        @Inject
        @MyQualifier
        Instance<String> foo;

    }

    @ApplicationScoped
    static class Producers {

        // @Produces is not added automatically
        @MyQualifier
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

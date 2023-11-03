package io.quarkus.arc.test.buildextension.beans;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SynthBeanWithWrongScopeTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new TestRegistrar()).shouldFail().build();

    @Test
    public void testInvalidScopeWasDetectedAtBuildTime() {
        assertNotNull(container.getFailure());
        assertTrue(container.getFailure().getMessage().contains("was defined with invalid scope annotation"),
                container.getFailure().getMessage());
    }

    // no bean defining annotation; registered synthetically
    static class BeanifyMe {

    }

    public static class MyBeanCreator implements BeanCreator<BeanifyMe> {

        @Override
        public BeanifyMe create(CreationalContext<BeanifyMe> creationalContext, Map<String, Object> params) {
            return new BeanifyMe();
        }
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext context) {
            context.configure(BeanifyMe.class).unremovable()
                    .types(BeanifyMe.class)
                    // assign annotation that isn't a known scope - this should lead to sensible exception
                    .scope(NotAScope.class)
                    .creator(MyBeanCreator.class).done();
        }

    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface NotAScope {

    }
}

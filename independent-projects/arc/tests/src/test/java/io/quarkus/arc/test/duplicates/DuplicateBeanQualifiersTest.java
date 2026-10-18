package io.quarkus.arc.test.duplicates;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class DuplicateBeanQualifiersTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class)
            .beanRegistrars(new MyBeanRegistrar())
            .build();

    @Test
    public void test() {
        assertNotNull(Arc.container().select(MyBean.class, new MyQualifier.Literal("foobar")).get());
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier {
        String value();

        final class Literal extends AnnotationLiteral<MyQualifier> implements MyQualifier {
            private final String value;

            public Literal(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }
        }
    }

    static class MyBean {
    }

    static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            return new MyBean();
        }
    }

    static class MyBeanRegistrar implements BeanRegistrar {
        @Override
        public void register(RegistrationContext context) {
            Index index = null;
            try {
                index = Index.of(MyBean.class, Object.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            context.configure(MyBean.class)
                    .addType(MyBean.class)
                    .addQualifier(AnnotationInstance.builder(MyQualifier.class).value("foobar")
                            .buildWithTarget(index.getClassByName(Object.class)))
                    .addQualifier(AnnotationInstance.builder(MyQualifier.class).value("foobar")
                            .buildWithTarget(index.getClassByName(MyBean.class)))
                    .creator(MyBeanCreator.class)
                    .done();
        }
    }
}

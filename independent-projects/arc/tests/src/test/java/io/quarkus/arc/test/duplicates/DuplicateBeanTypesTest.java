package io.quarkus.arc.test.duplicates;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class DuplicateBeanTypesTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new MyBeanRegistrar())
            .build();

    @Test
    public void test() {
        assertNotNull(Arc.container().select(MyBean.class).get());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface MyAnnotation {
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
            context.configure(MyBean.class)
                    .addType(ClassType.create(MyBean.class))
                    .addType(ClassType.builder(MyBean.class)
                            .addAnnotation(AnnotationInstance.builder(MyAnnotation.class).build())
                            .build())
                    .creator(MyBeanCreator.class)
                    .done();
        }
    }
}

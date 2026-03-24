package io.quarkus.arc.test.duplicates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class DuplicateBeanInjectionPointQualifiersTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class, MyDependency.class)
            .beanRegistrars(new MyBeanRegistrar())
            .strictCompatibility(true) // we only generate `Bean.getInjectionPoints()` in strict mode
            .build();

    @Test
    public void test() {
        Set<InjectionPoint> ips = Arc.container().select(MyBean.class).getHandle().getBean().getInjectionPoints();
        assertNotNull(ips);
        assertEquals(1, ips.size());
        InjectionPoint ip = ips.iterator().next();
        assertEquals(MyDependency.class, ip.getType());
        assertEquals(Set.of(new MyQualifier.Literal()), ip.getQualifiers());
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier {
        final class Literal extends AnnotationLiteral<MyQualifier> implements MyQualifier {
        }
    }

    @Dependent
    @MyQualifier
    static class MyDependency {
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
                index = Index.of(MyDependency.class, Object.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            ClassInfo objectClass = index.getClassByName(Object.class);
            ClassInfo myDependencyClass = index.getClassByName(MyDependency.class);

            context.configure(MyBean.class)
                    .addType(MyBean.class)
                    .addInjectionPoint(ClassType.create(MyDependency.class),
                            AnnotationInstance.builder(MyQualifier.class).buildWithTarget(objectClass),
                            AnnotationInstance.builder(MyQualifier.class).buildWithTarget(myDependencyClass))
                    .creator(MyBeanCreator.class)
                    .done();
        }
    }
}

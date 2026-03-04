package io.quarkus.arc.test.duplicates;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Stereotype;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.StereotypeInfo;
import io.quarkus.arc.test.ArcTestContainer;

public class DuplicateBeanStereotypesTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyStereotype.class)
            .beanRegistrars(new MyBeanRegistrar())
            .build();

    @Test
    public void test() {
        assertNotNull(Arc.container().select(MyBean.class).get());
    }

    @Stereotype
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyStereotype {
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
            ClassInfo clazz;
            try {
                clazz = Index.singleClass(MyStereotype.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            Supplier<StereotypeInfo> stereotypeSupplier = () -> new StereotypeInfo(BuiltinScope.DEPENDENT.getInfo(),
                    List.of(), false, null, false, false, clazz, false, List.of());

            context.configure(MyBean.class)
                    .addType(MyBean.class)
                    .addStereotype(stereotypeSupplier.get())
                    .addStereotype(stereotypeSupplier.get())
                    .creator(MyBeanCreator.class)
                    .done();
        }
    }
}

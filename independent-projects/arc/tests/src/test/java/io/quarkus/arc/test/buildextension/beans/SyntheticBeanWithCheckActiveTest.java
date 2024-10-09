package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;

import org.jboss.jandex.ClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticBeanWithCheckActiveTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure(MyBean.class)
                            .addType(ClassType.create(MyBean.class))
                            .creator(MyBeanCreator.class)
                            .checkActive(MyBeanIsActive.class)
                            .unremovable()
                            .done();
                }
            })
            .build();

    @Test
    public void test() {
        InjectableInstance<MyBean> myBean = Arc.container().select(MyBean.class);

        MyBeanIsActive.active = true;

        assertTrue(myBean.getHandle().getBean().isActive());
        assertNull(myBean.getHandle().getBean().checkActive().inactiveReason());
        assertNotNull(myBean.get());

        MyBeanIsActive.active = false;

        assertFalse(myBean.getHandle().getBean().isActive());
        assertNotNull(myBean.getHandle().getBean().checkActive().inactiveReason());
        InactiveBeanException e = assertThrows(InactiveBeanException.class, myBean::get);
        assertTrue(e.getMessage().contains("Bean is not active"));
        assertTrue(e.getMessage().contains("MyBean not active"));
        assertTrue(e.getMessage().contains("Deeper reason"));
    }

    public static class MyBean {
    }

    public static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            return new MyBean();
        }
    }

    public static class MyBeanIsActive implements Supplier<ActiveResult> {
        public static boolean active;

        @Override
        public ActiveResult get() {
            if (active) {
                return ActiveResult.active();
            } else {
                return ActiveResult.inactive("MyBean not active", ActiveResult.inactive("Deeper reason"));
            }
        }
    }
}

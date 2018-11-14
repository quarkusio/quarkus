package org.jboss.protean.arc.test.build.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.BeanCreator;
import org.jboss.protean.arc.processor.BeanConfigurator;
import org.jboss.protean.arc.processor.BeanRegistrar;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.junit.Rule;
import org.junit.Test;

public class BeanRegistrarTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(UselessBean.class).beanRegistrars(new TestRegistrar()).build();

    @Test
    public void testSyntheticBean() {
        assertEquals(Integer.valueOf(152), Arc.container().instance(Integer.class).get());
        assertEquals("Hello Frantisek!", Arc.container().instance(String.class).get());
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public boolean initialize(BuildContext buildContext) {
            assertTrue(buildContext.get(Key.INDEX).getKnownClasses().stream().anyMatch(cl -> cl.name().toString().equals(UselessBean.class.getName())));
            return true;
        }

        @Override
        public void register(RegistrationContext registrationContext) {
            // Verify that the class bean was registered
            assertTrue(registrationContext.get(Key.BEANS).stream()
                    .anyMatch(b -> b.isClassBean() && b.getBeanClass().toString().equals(UselessBean.class.getName())));

            BeanConfigurator<Integer> integerConfigurator = registrationContext.configure(Integer.class);
            integerConfigurator.types(Integer.class).creator(mc -> {
                ResultHandle ret = mc.newInstance(MethodDescriptor.ofConstructor(Integer.class, int.class), mc.load(152));
                mc.returnValue(ret);
            });
            integerConfigurator.done();

            registrationContext.configure(String.class).types(String.class).param("name", "Frantisek").creator(StringCreator.class).done();
        }

    }

    public static class StringCreator implements BeanCreator<String> {

        @Override
        public String create(CreationalContext<String> creationalContext, Map<String, Object> params) {
            return "Hello " + params.get("name") + "!";
        }

    }

    @ApplicationScoped
    static class UselessBean {

    }

}

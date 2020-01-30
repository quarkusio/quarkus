package io.quarkus.arc.test.buildextension.beans;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BeanRegistrarTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(UselessBean.class, MyQualifier.class, NextQualifier.class)
            .removeUnusedBeans(true)
            .beanRegistrars(new TestRegistrar()).build();

    @SuppressWarnings("serial")
    static class NextQualifierLiteral extends AnnotationLiteral<NextQualifier> implements NextQualifier {

        @Override
        public String name() {
            return "Roman";
        }

        @Override
        public int age() {
            return 42;
        }

        @Override
        public Class<?>[] classes() {
            return new Class[] { String.class };
        }

    }

    @Test
    public void testSyntheticBean() {
        assertEquals(Integer.valueOf(152), Arc.container().instance(Integer.class).get());
        assertEquals("Hello Frantisek!", Arc.container().instance(String.class).get());
        assertEquals("Hello Roman!", Arc.container().instance(String.class, new NextQualifierLiteral()).get());
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public boolean initialize(BuildContext buildContext) {
            assertTrue(buildContext.get(Key.INDEX).getKnownClasses().stream()
                    .anyMatch(cl -> cl.name().toString().equals(UselessBean.class.getName())));
            return true;
        }

        @Override
        public void register(RegistrationContext context) {
            Optional<BeanInfo> uselessBean = context.beans().withBeanClass(UselessBean.class).firstResult();
            assertTrue(uselessBean.isPresent());
            assertTrue(context.beans().findByIdentifier(uselessBean.get().getIdentifier()).isPresent());
            assertEquals(uselessBean.get().getIdentifier(),
                    context.beans().withQualifier(MyQualifier.class).firstResult().get().getIdentifier());

            BeanConfigurator<Integer> integerConfigurator = context.configure(Integer.class);
            integerConfigurator.unremovable().types(Integer.class).creator(mc -> {
                ResultHandle ret = mc.newInstance(MethodDescriptor.ofConstructor(Integer.class, int.class), mc.load(152));
                mc.returnValue(ret);
            });
            integerConfigurator.done();

            context.configure(String.class).unremovable().types(String.class).param("name", "Frantisek")
                    .creator(StringCreator.class).done();

            context.configure(String.class).types(String.class).param("name", "Roman")
                    .creator(StringCreator.class).addQualifier().annotation(NextQualifier.class).addValue("name", "Roman")
                    .addValue("age", 42)
                    .addValue("classes", new Class[] { String.class }).done().unremovable().done();
        }

    }

    public static class StringCreator implements BeanCreator<String> {

        @Override
        public String create(CreationalContext<String> creationalContext, Map<String, Object> params) {
            return "Hello " + params.get("name") + "!";
        }

    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface MyQualifier {

    }

    @MyQualifier
    @ApplicationScoped
    static class UselessBean {

    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface NextQualifier {

        String name();

        int age();

        Class<?>[] classes();

    }

}

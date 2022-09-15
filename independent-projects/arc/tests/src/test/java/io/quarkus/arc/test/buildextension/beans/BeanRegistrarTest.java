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
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BeanRegistrarTest {

    public static volatile boolean beanDestroyerInvoked = false;

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(UselessBean.class, MyQualifier.class, NextQualifier.class, ListConsumer.class)
            .removeUnusedBeans(true)
            .addRemovalExclusion(b -> b.hasType(DotName.createSimple(ListConsumer.class.getName())))
            .beanRegistrars(new TestRegistrar()).build();

    @AfterAll
    public static void assertDestroyerInvoked() {
        Assertions.assertTrue(beanDestroyerInvoked);
    }

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
        @SuppressWarnings({ "resource" })
        List<String> list = Arc.container().instance(ListConsumer.class).get().list;
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0));

        InjectableInstance<Long> instance = Arc.container().select(Long.class);
        // test configurator with higher priority
        assertEquals(42l, instance.iterator().next());
        long val = 0;
        for (Long v : instance) {
            val += v;
        }
        assertEquals(64l, val);
    }

    @Singleton
    static class ListConsumer {

        @Inject
        List<String> list;

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
            integerConfigurator.unremovable();
            integerConfigurator.types(Integer.class).creator(mc -> {
                ResultHandle ret = mc.newInstance(MethodDescriptor.ofConstructor(Integer.class, int.class), mc.load(152));
                mc.returnValue(ret);
            });
            integerConfigurator.destroyer(SimpleDestroyer.class);
            integerConfigurator.scope(Singleton.class);
            integerConfigurator.done();

            context.configure(String.class)
                    .unremovable()
                    .types(String.class)
                    .param("name", "Frantisek")
                    .creator(StringCreator.class)
                    .done();

            context.configure(String.class).types(String.class).param("name", "Roman")
                    .creator(StringCreator.class)
                    .addQualifier().annotation(NextQualifier.class).addValue("name", "Roman").addValue("age", 42)
                    .addValue("classes", new Class[] { String.class }).done()
                    .unremovable()
                    .done();

            context.configure(List.class)
                    // List, List<String>
                    .addType(Type.create(DotName.createSimple(List.class.getName()), Kind.CLASS))
                    .addType(ParameterizedType.create(DotName.createSimple(List.class.getName()),
                            new Type[] { Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS) }, null))
                    .creator(ListCreator.class)
                    .unremovable()
                    .done();

            context.configure(Long.class)
                    .addType(Long.class)
                    .priority(10)
                    .creator(mc -> mc.returnValue(mc.load(42l)))
                    .unremovable()
                    .done();

            context.configure(Long.class)
                    .addType(Long.class)
                    .addType(Double.class)
                    .priority(5)
                    .creator(mc -> mc.returnValue(mc.load(22l)))
                    .unremovable()
                    .done();

            uselessBean = context.beans()
                    .assignableTo(
                            Type.create(DotName.createSimple(UselessBean.class.getName()), org.jboss.jandex.Type.Kind.CLASS))
                    .firstResult();
            assertTrue(uselessBean.isPresent());
            assertEquals(UselessBean.class.getName(), uselessBean.get().getBeanClass().toString());
        }

    }

    public static class StringCreator implements BeanCreator<String> {

        @Override
        public String create(CreationalContext<String> creationalContext, Map<String, Object> params) {
            return "Hello " + params.get("name") + "!";
        }

    }

    public static class ListCreator implements BeanCreator<List<String>> {

        @Override
        public List<String> create(CreationalContext<List<String>> creationalContext, Map<String, Object> params) {
            return List.of("foo");
        }

    }

    public static class SimpleDestroyer implements BeanDestroyer<Integer> {

        @Override
        public void destroy(Integer instance, CreationalContext<Integer> creationalContext, Map<String, Object> params) {
            beanDestroyerInvoked = true;
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

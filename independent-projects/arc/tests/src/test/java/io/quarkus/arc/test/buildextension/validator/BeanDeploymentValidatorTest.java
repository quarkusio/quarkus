package io.quarkus.arc.test.buildextension.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.ObserverInfo;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BeanDeploymentValidatorTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Alpha.class, UselessBean.class)
            .beanRegistrars(new TestRegistrar())
            .removeUnusedBeans(true)
            .beanDeploymentValidators(new TestValidator()).build();

    @Test
    public void testValidator() {
        assertTrue(Arc.container().instance(Alpha.class).get().getStrings().isEmpty());
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext registrationContext) {
            registrationContext.configure(List.class).types(EmptyStringListCreator.listStringType())
                    .creator(EmptyStringListCreator.class).done();
        }

    }

    static class TestValidator implements BeanDeploymentValidator {

        @Override
        public void validate(ValidationContext context) {
            assertTrue(context.getInjectionPoints().stream().filter(InjectionPointInfo::isProgrammaticLookup)
                    .filter(ip -> ip.getTarget().kind() == org.jboss.jandex.AnnotationTarget.Kind.FIELD
                            && ip.getTarget().asField().name().equals("foo"))
                    .findFirst().isPresent());

            assertFalse(context.removedBeans().withBeanClass(UselessBean.class).isEmpty());

            assertFalse(context.beans().classBeans().withBeanClass(Alpha.class).isEmpty());
            assertFalse(context.beans().syntheticBeans().withBeanType(EmptyStringListCreator.listStringType())
                    .isEmpty());
            List<BeanInfo> namedAlpha = context.beans().withName("alpha").collect();
            assertEquals(1, namedAlpha.size());
            assertEquals(Alpha.class.getName(), namedAlpha.get(0).getBeanClass().toString());

            Collection<ObserverInfo> observers = context.get(Key.OBSERVERS);

            List<BeanInfo> namedClassWithObservers = context.beans().classBeans().namedBeans().stream()
                    .filter(b -> observers.stream().anyMatch(o -> o.getDeclaringBean().equals(b))).collect(Collectors.toList());
            assertEquals(1, namedClassWithObservers.size());
            assertEquals(Alpha.class.getName(), namedClassWithObservers.get(0).getBeanClass().toString());

            assertTrue(observers
                    .stream()
                    .anyMatch(o -> o.getObservedType()
                            .equals(Type.create(DotName.createSimple(Object.class.getName()), Kind.CLASS))));
            // We do not test a validation problem - ArcTestContainer rule would fail
        }

    }

    @Named
    @ApplicationScoped
    static class Alpha {

        @Inject
        List<String> strings;

        @Inject
        Instance<String> foo;

        void observeAppContextInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        }

        List<String> getStrings() {
            return strings;
        }

    }

    public static class EmptyStringListCreator implements BeanCreator<List<String>> {

        static Type listStringType() {
            Type[] args = new Type[1];
            args[0] = Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS);
            return ParameterizedType.create(DotName.createSimple(List.class.getName()), args, null);
        }

        @Override
        public List<String> create(CreationalContext<List<String>> creationalContext, Map<String, Object> params) {
            return Collections.emptyList();
        }

    }

    @ApplicationScoped
    static class UselessBean {

    }

}

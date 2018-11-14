package org.jboss.protean.arc.test.build.processor;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.BeanCreator;
import org.jboss.protean.arc.processor.BeanDeploymentValidator;
import org.jboss.protean.arc.processor.BeanRegistrar;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class BeanDeploymentValidatorTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Alpha.class).beanRegistrars(new TestRegistrar())
            .beanDeploymentValidators(new TestValidator()).build();

    @Test
    public void testValidator() {
        assertTrue(Arc.container().instance(Alpha.class).get().getStrings().isEmpty());
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext registrationContext) {
            registrationContext.configure(List.class).types(EmptyStringListCreator.listStringType()).creator(EmptyStringListCreator.class).done();
        }

    }

    static class TestValidator implements BeanDeploymentValidator {

        private BuildContext buildContext;

        @Override
        public boolean initialize(BuildContext buildContext) {
            this.buildContext = buildContext;
            return true;
        }

        @Override
        public void validate() {
            assertTrue(buildContext.get(Key.BEANS).stream().anyMatch(b -> b.isClassBean() && b.getBeanClass().toString().equals(Alpha.class.getName())));
            assertTrue(buildContext.get(Key.BEANS).stream().anyMatch(b -> b.isSynthetic() && b.getTypes().contains(EmptyStringListCreator.listStringType())));
            assertTrue(buildContext.get(Key.OBSERVERS).stream()
                    .anyMatch(o -> o.getObservedType().equals(Type.create(DotName.createSimple(Object.class.getName()), Kind.CLASS))));
            // We do not test a validation problem - ArcTestContainer rule would fail
        }

    }

    @ApplicationScoped
    static class Alpha {

        @Inject
        private List<String> strings;

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

}

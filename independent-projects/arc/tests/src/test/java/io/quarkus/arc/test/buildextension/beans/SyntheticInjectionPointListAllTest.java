package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.All;
import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticInjectionPointListAllTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(SomeBean.class)
            .beanRegistrars(new TestRegistrar()).build();

    @Test
    public void testListAllInjection() {
        SyntheticBean synthBean = Arc.container().instance(SyntheticBean.class).get();
        assertNotNull(synthBean);
        List<SomeBean> list = synthBean.getList();
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Singleton
    static class SomeBean {

        public String ping() {
            return SomeBean.class.getSimpleName();
        }

    }

    static class SyntheticBean {

        private List<SomeBean> list;

        public SyntheticBean(List<SomeBean> list) {
            this.list = list;
        }

        public List<SomeBean> getList() {
            return list;
        }
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext context) {
            context.configure(SyntheticBean.class)
                    .addType(ClassType.create(DotName.createSimple(SyntheticBean.class)))
                    .creator(SynthBeanCreator.class)
                    // add injection point for @All List<SomeBean>
                    .addInjectionPoint(ParameterizedType.create(List.class, ClassType.create(SomeBean.class)),
                            AnnotationInstance.builder(All.class).build())
                    .unremovable()
                    .done();
        }

    }

    public static class SynthBeanCreator implements BeanCreator<SyntheticBean> {

        @Override
        public SyntheticBean create(SyntheticCreationalContext<SyntheticBean> context) {
            return new SyntheticBean(context.getInjectedReference(new TypeLiteral<List<SomeBean>>() {
            }, All.Literal.INSTANCE));
        }

    }
}

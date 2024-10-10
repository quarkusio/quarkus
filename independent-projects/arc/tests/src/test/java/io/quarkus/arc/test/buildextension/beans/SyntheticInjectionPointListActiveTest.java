package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.ParameterizedType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Active;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.MyQualifier;

public class SyntheticInjectionPointListActiveTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class)
            .beanRegistrars(context -> {
                context.configure(MyBean.class)
                        .types(MyBean.class)
                        .qualifiers(AnnotationInstance.builder(MyQualifier.class).build())
                        .scope(Singleton.class)
                        .checkActive(AlwaysActive.class)
                        .creator(MyBeanCreator.class)
                        .done();

                context.configure(MyBean.class)
                        .types(MyBean.class)
                        .scope(Singleton.class)
                        .checkActive(NeverActive.class)
                        .creator(MyBeanCreator.class)
                        .done();

                context.configure(SyntheticBean.class)
                        .types(ClassType.create(SyntheticBean.class))
                        .creator(SyntheticBeanCreator.class)
                        .addInjectionPoint(ParameterizedType.create(List.class, ClassType.create(MyBean.class)),
                                AnnotationInstance.builder(Active.class).build())
                        .unremovable()
                        .done();
            })
            .build();

    @Test
    public void testListActiveInjection() {
        SyntheticBean syntheticBean = Arc.container().instance(SyntheticBean.class).get();
        assertNotNull(syntheticBean);
        List<MyBean> list = syntheticBean.getList();
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    static class MyBean {
        public String ping() {
            return MyBean.class.getSimpleName();
        }
    }

    static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            return new MyBean();
        }
    }

    static class SyntheticBean {
        private List<MyBean> list;

        SyntheticBean(List<MyBean> list) {
            this.list = list;
        }

        List<MyBean> getList() {
            return list;
        }
    }

    static class SyntheticBeanCreator implements BeanCreator<SyntheticBean> {
        @Override
        public SyntheticBean create(SyntheticCreationalContext<SyntheticBean> context) {
            return new SyntheticBean(context.getInjectedReference(new TypeLiteral<>() {
            }, Active.Literal.INSTANCE));
        }
    }

    static class AlwaysActive implements Supplier<ActiveResult> {
        @Override
        public ActiveResult get() {
            return ActiveResult.active();
        }
    }

    static class NeverActive implements Supplier<ActiveResult> {
        @Override
        public ActiveResult get() {
            return ActiveResult.inactive("");
        }
    }
}

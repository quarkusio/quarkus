package io.quarkus.arc.test.stereotypes;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StereotypeInheritanceTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyStereotype.class, MyBeanDefiningAnnotation.class,
            MySuperclass.class, MyBean.class);

    @Test
    public void test() {
        ArcContainer container = Arc.container();
        BeanManager bm = container.beanManager();

        String beanId;

        {
            container.requestContext().activate();
            MyBean bean = container.instance(MyBean.class).get();
            assertNotNull(bean);
            beanId = bean.getId();
            assertNotNull(beanId);

            Set<Bean<?>> metadata = bm.getBeans(MyBean.class);
            assertEquals(1, metadata.size());
            assertEquals(RequestScoped.class, metadata.iterator().next().getScope());

            container.requestContext().deactivate();
        }

        {
            container.requestContext().activate();
            MyBean bean = container.instance(MyBean.class).get();
            assertNotNull(bean);
            assertNotNull(bean.getId());
            assertNotEquals(beanId, bean.getId());

            Set<Bean<?>> metadata = bm.getBeans(MyBean.class);
            assertEquals(1, metadata.size());
            assertEquals(RequestScoped.class, metadata.iterator().next().getScope());

            container.requestContext().deactivate();
        }
    }

    @RequestScoped
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @Inherited
    @interface MyStereotype {
    }

    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface MyBeanDefiningAnnotation {
    }

    @MyStereotype
    static class MySuperclass {
    }

    @MyBeanDefiningAnnotation
    static class MyBean extends MySuperclass {
        private String id;

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

        public String getId() {
            return id;
        }
    }
}
package io.quarkus.arc.test.stereotypes;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;

public class TransitiveStereotypeTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyStereotype.class, MyOtherStereotype.class, MyBean.class);

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
            assertMetadata((InjectableBean<?>) metadata.iterator().next());

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
            assertMetadata((InjectableBean<?>) metadata.iterator().next());

            container.requestContext().deactivate();
        }
    }

    private void assertMetadata(InjectableBean<?> bean) {
        assertEquals(RequestScoped.class, bean.getScope());
        assertEquals("myBean", bean.getName());
        assertTrue(bean.isAlternative());
        assertEquals(123, bean.getPriority());
    }

    @RequestScoped
    @Named
    @Alternative
    @Priority(123)
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface MyStereotype {
    }

    @MyStereotype
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface MyOtherStereotype {
    }

    @MyOtherStereotype
    static class MyBean {
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

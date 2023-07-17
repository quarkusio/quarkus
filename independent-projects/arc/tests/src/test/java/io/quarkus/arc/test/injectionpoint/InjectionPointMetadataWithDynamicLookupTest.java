package io.quarkus.arc.test.injectionpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class InjectionPointMetadataWithDynamicLookupTest {
    @RegisterExtension
    private ArcTestContainer container = new ArcTestContainer(BeanWithInjectionPointMetadata.class,
            MyDependentBean.class, MySingletonBean.class);

    @Test
    public void arcContainerInstance() {
        // the "current" injection point of `Arc.container().instance(...)` doesn't seem to be well defined
        BeanWithInjectionPointMetadata bean = Arc.container().instance(BeanWithInjectionPointMetadata.class).get();

        // this is probably an implementation artifact, not an intentional choice
        bean.assertPresent(ip -> {
            assertEquals(Object.class, ip.getType());
            assertEquals(Set.of(), ip.getQualifiers());
            assertNull(ip.getMember());
            assertNull(ip.getBean());
        });
    }

    @Test
    public void arcContainerSelect() {
        BeanWithInjectionPointMetadata bean = Arc.container().select(BeanWithInjectionPointMetadata.class).get();

        bean.assertPresent(ip -> {
            // the `Instance<BeanWithInjectionPointMetadata>` through which the `bean` was looked up
            assertEquals(BeanWithInjectionPointMetadata.class, ip.getType());
            assertEquals(Set.of(), ip.getQualifiers());
            assertNull(ip.getMember());
            assertNull(ip.getBean());
        });
    }

    @Test
    public void cdiCurrentSelect() {
        BeanWithInjectionPointMetadata bean = CDI.current().select(BeanWithInjectionPointMetadata.class).get();

        bean.assertPresent(ip -> {
            // the `Instance<BeanWithInjectionPointMetadata>` through which the `bean` was looked up
            assertEquals(BeanWithInjectionPointMetadata.class, ip.getType());
            assertEquals(Set.of(), ip.getQualifiers());
            assertNull(ip.getMember());
            assertNull(ip.getBean());
        });
    }

    @Test
    public void beanManagerCreateInstanceAndSelect() {
        BeanManager bm = Arc.container().beanManager();
        BeanWithInjectionPointMetadata bean = bm.createInstance().select(BeanWithInjectionPointMetadata.class).get();

        bean.assertPresent(ip -> {
            // the `Instance<BeanWithInjectionPointMetadata>` through which the `bean` was looked up
            assertEquals(BeanWithInjectionPointMetadata.class, ip.getType());
            assertEquals(Set.of(), ip.getQualifiers());
            assertNull(ip.getMember());
            assertNull(ip.getBean());
        });
    }

    @Test
    public void beanManagerGetReference() {
        BeanManager bm = Arc.container().beanManager();
        Bean<BeanWithInjectionPointMetadata> bean = (Bean<BeanWithInjectionPointMetadata>) bm.resolve(
                bm.getBeans(BeanWithInjectionPointMetadata.class));
        CreationalContext<BeanWithInjectionPointMetadata> cc = bm.createCreationalContext(bean);
        BeanWithInjectionPointMetadata instance = (BeanWithInjectionPointMetadata) bm.getReference(
                bean, BeanWithInjectionPointMetadata.class, cc);

        instance.assertAbsent();
    }

    @Test
    public void beanManagerGetInjectableReference() {
        InjectionPoint lookup = new DummyInjectionPoint(BeanWithInjectionPointMetadata.class, Default.Literal.INSTANCE);

        BeanManager bm = Arc.container().beanManager();
        CreationalContext<BeanWithInjectionPointMetadata> cc = bm.createCreationalContext(null);
        BeanWithInjectionPointMetadata instance = (BeanWithInjectionPointMetadata) bm.getInjectableReference(lookup, cc);

        instance.assertPresent(ip -> {
            assertSame(lookup, ip);
        });
    }

    @Test
    public void injectionIntoDependentBean() {
        MyDependentBean bean = Arc.container().select(MyDependentBean.class).get();

        // the `Instance<MyDependentBean>` through which the `bean` was looked up
        assertEquals(MyDependentBean.class, bean.ip.getType());
        assertEquals(Set.of(), bean.ip.getQualifiers());
        assertNull(bean.ip.getMember());
        assertNull(bean.ip.getBean());

        assertNotNull(bean.dependency);
        bean.dependency.assertPresent(ip -> {
            assertEquals(BeanWithInjectionPointMetadata.class, ip.getType());
            assertEquals(Set.of(Default.Literal.INSTANCE), ip.getQualifiers());
            assertEquals(MyDependentBean.class, ip.getMember().getDeclaringClass());
            assertEquals("dependency", ip.getMember().getName());
            assertEquals(MyDependentBean.class, ip.getBean().getBeanClass());
        });

        assertNotNull(bean.dependencyInstance);
        bean.dependencyInstance.get().assertPresent(ip -> {
            assertEquals(BeanWithInjectionPointMetadata.class, ip.getType());
            assertEquals(Set.of(Default.Literal.INSTANCE), ip.getQualifiers());
            assertEquals(MyDependentBean.class, ip.getMember().getDeclaringClass());
            assertEquals("dependencyInstance", ip.getMember().getName());
            assertEquals(MyDependentBean.class, ip.getBean().getBeanClass());
        });
    }

    @Test
    public void injectionIntoSingletonBean() {
        MySingletonBean bean = Arc.container().select(MySingletonBean.class).get();

        assertNotNull(bean.dependency);
        bean.dependency.assertPresent(ip -> {
            assertEquals(BeanWithInjectionPointMetadata.class, ip.getType());
            assertEquals(Set.of(Default.Literal.INSTANCE), ip.getQualifiers());
            assertEquals(MySingletonBean.class, ip.getMember().getDeclaringClass());
            assertEquals("dependency", ip.getMember().getName());
            assertEquals(MySingletonBean.class, ip.getBean().getBeanClass());
        });
        assertNotNull(bean.dependencyInstance);
        bean.dependencyInstance.get().assertPresent(ip -> {
            assertEquals(BeanWithInjectionPointMetadata.class, ip.getType());
            assertEquals(Set.of(Default.Literal.INSTANCE), ip.getQualifiers());
            assertEquals(MySingletonBean.class, ip.getMember().getDeclaringClass());
            assertEquals("dependencyInstance", ip.getMember().getName());
            assertEquals(MySingletonBean.class, ip.getBean().getBeanClass());
        });
    }

    @Dependent
    static class MyDependentBean {
        @Inject
        InjectionPoint ip;

        @Inject
        BeanWithInjectionPointMetadata dependency;

        @Inject
        Instance<BeanWithInjectionPointMetadata> dependencyInstance;
    }

    @Singleton
    static class MySingletonBean {
        @Inject
        BeanWithInjectionPointMetadata dependency;

        @Inject
        Instance<BeanWithInjectionPointMetadata> dependencyInstance;
    }

}

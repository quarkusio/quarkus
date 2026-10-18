package io.quarkus.arc.test.bean.destroy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class BeanPreDestroyErrorTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(DestroyBean.class, DestroyProducerBean.class);

    @BeforeEach
    public void setup() {
        DestroyBean.destroyed = false;
        DestroyProducerBean.destroyed = false;
    }

    @Test
    public void testErrorSwallowed() {
        // Test that an exception is not rethrown
        InstanceHandle<DestroyBean> beanInstance = Arc.container().instance(DestroyBean.class);
        assertEquals(42, beanInstance.get().ping());
        assertFalse(DestroyBean.destroyed);
        beanInstance.destroy();
        assertTrue(DestroyBean.destroyed);

        InstanceHandle<BigDecimal> bigInstance = Arc.container().instance(BigDecimal.class);
        assertEquals(BigDecimal.ONE, bigInstance.get());
        assertFalse(DestroyProducerBean.destroyed);
        bigInstance.destroy();
        assertTrue(DestroyProducerBean.destroyed);
    }

    @Test
    public void testErrorSwallowedWithLowLevelApi() {
        BeanManager bm = Arc.container().beanManager();

        {
            Bean<DestroyBean> bean = (Bean<DestroyBean>) bm.resolve(bm.getBeans(DestroyBean.class));
            CreationalContext<DestroyBean> ctx = bm.createCreationalContext(bean);
            DestroyBean instance = bean.create(ctx);
            assertEquals(42, instance.ping());
            assertFalse(DestroyBean.destroyed);
            bean.destroy(instance, ctx);
            assertTrue(DestroyBean.destroyed);
        }

        {
            Bean<BigDecimal> bean = (Bean<BigDecimal>) bm.resolve(bm.getBeans(BigDecimal.class));
            CreationalContext<BigDecimal> ctx = bm.createCreationalContext(bean);
            BigDecimal instance = bean.create(ctx);
            assertEquals(BigDecimal.ONE, instance);
            assertFalse(DestroyProducerBean.destroyed);
            bean.destroy(instance, ctx);
            assertTrue(DestroyProducerBean.destroyed);
        }
    }

    @Singleton
    static class DestroyBean {
        static boolean destroyed;

        int ping() {
            return 42;
        }

        @PreDestroy
        void destroy() {
            destroyed = true;
            throw new IllegalStateException();
        }

    }

    @Dependent
    static class DestroyProducerBean {
        static boolean destroyed;

        @Produces
        BigDecimal produce() {
            return BigDecimal.ONE;
        }

        void dispose(@Disposes BigDecimal val) {
            destroyed = true;
            throw new IllegalStateException();
        }

    }
}

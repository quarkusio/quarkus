package io.quarkus.arc.test.bean.destroy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BeanPreDestroyErrorTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(DestroyBean.class, DestroyProducerBean.class);

    @Test
    public void testErrorSwallowed() {
        // Test that an exception is not rethrown
        InstanceHandle<DestroyBean> beanInstance = Arc.container().instance(DestroyBean.class);
        assertEquals(42, beanInstance.get().ping());
        beanInstance.destroy();

        InstanceHandle<BigDecimal> bigInstance = Arc.container().instance(BigDecimal.class);
        assertEquals(BigDecimal.ONE, bigInstance.get());
        bigInstance.destroy();
    }

    @Singleton
    static class DestroyBean {

        int ping() {
            return 42;
        }

        @PreDestroy
        void destroy() {
            throw new IllegalStateException();
        }

    }

    @Dependent
    static class DestroyProducerBean {

        @Produces
        BigDecimal produce() {
            return BigDecimal.ONE;
        }

        void dispose(@Disposes BigDecimal val) {
            throw new IllegalStateException();
        }

    }
}

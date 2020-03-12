package io.quarkus.arc.test.bean.destroy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import java.math.BigDecimal;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
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

package io.quarkus.arc.test.instance.frombean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InstanceFromBeanTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Alpha.class);

    @SuppressWarnings("unchecked")
    @Test
    public void testDestroy() {
        InjectableBean<Alpha> bean1 = (InjectableBean<Alpha>) Arc.container().beanManager().getBeans(Alpha.class).iterator()
                .next();
        InjectableBean<Alpha> bean2 = Arc.container().bean(bean1.getIdentifier());
        assertEquals(bean1, bean2);
        assertEquals(Arc.container().instance(bean2).get().getId(), Arc.container().instance(bean2).get().getId());
    }

    @Singleton
    static class Alpha {

        private String id;

        @PostConstruct
        void init() {
            this.id = UUID.randomUUID().toString();
        }

        String getId() {
            return id;
        }

    }

}

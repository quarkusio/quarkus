package io.quarkus.arc.test.beanmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BeanManagerInstanceTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(FuuService.class);

    @Test
    public void testGetEvent() {
        BeanManager beanManager = Arc.container()
                .beanManager();
        Instance<FuuService> instance = beanManager.createInstance()
                .select(FuuService.class);
        assertTrue(instance.isResolvable());
        assertEquals(10, instance.get().age);
    }

    @Dependent
    static class FuuService {

        int age;

        @PostConstruct
        void init() {
            age = 10;
        }

    }

}

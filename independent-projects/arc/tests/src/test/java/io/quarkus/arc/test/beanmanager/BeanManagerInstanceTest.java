package io.quarkus.arc.test.beanmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import org.junit.Rule;
import org.junit.Test;

public class BeanManagerInstanceTest {

    @Rule
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

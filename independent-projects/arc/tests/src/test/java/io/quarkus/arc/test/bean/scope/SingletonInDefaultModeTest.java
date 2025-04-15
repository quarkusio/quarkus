package io.quarkus.arc.test.bean.scope;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class SingletonInDefaultModeTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(MyBean.class);

    @Test
    public void test() {
        InstanceHandle<MyBean> myBean = Arc.container().instance(MyBean.class);
        assertTrue(myBean.isAvailable());
        assertNotNull(myBean.get());

        assertDoesNotThrow(() -> {
            Arc.container().select(MyBean.class).get();
        });
    }

    @Singleton
    static class MyBean {
    }
}

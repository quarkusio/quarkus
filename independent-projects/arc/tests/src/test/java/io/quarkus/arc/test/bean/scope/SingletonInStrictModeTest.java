package io.quarkus.arc.test.bean.scope;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class SingletonInStrictModeTest {
    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyBean.class)
            .strictCompatibility(true)
            .build();

    @Test
    public void test() {
        InstanceHandle<MyBean> myBean = Arc.container().instance(MyBean.class);
        assertFalse(myBean.isAvailable());
        assertNull(myBean.get());

        assertThrows(UnsatisfiedResolutionException.class, () -> {
            Arc.container().select(MyBean.class).get();
        });
    }

    @Singleton
    static class MyBean {
    }
}

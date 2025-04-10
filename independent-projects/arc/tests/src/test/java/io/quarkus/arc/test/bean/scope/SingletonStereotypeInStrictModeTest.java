package io.quarkus.arc.test.bean.scope;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class SingletonStereotypeInStrictModeTest {
    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(SingletonScoped.class, MyBean.class)
            .strictCompatibility(true)
            .build();

    @Test
    public void test() {
        InstanceHandle<MyBean> myBean = Arc.container().instance(MyBean.class);
        assertNotNull(myBean.get());
        assertEquals(Singleton.class, myBean.getBean().getScope());

        assertDoesNotThrow(() -> {
            Arc.container().select(MyBean.class).get();
        });
    }

    @Singleton
    @Stereotype
    @Retention(RetentionPolicy.RUNTIME)
    @interface SingletonScoped {
    }

    @SingletonScoped
    static class MyBean {
    }
}

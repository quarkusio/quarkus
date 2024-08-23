package io.quarkus.arc.test.lock;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Lock;
import io.quarkus.arc.Lock.Type;
import io.quarkus.arc.impl.LockInterceptor;
import io.quarkus.arc.test.ArcTestContainer;

public class LockInterceptorDeadlockTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(SimpleApplicationScopedBean.class, Lock.class,
            LockInterceptor.class);

    @Test
    public void testApplicationScopedBean() throws Exception {
        SimpleApplicationScopedBean bean = Arc.container().instance(SimpleApplicationScopedBean.class).get();
        assertTrue(bean.read());
        assertTrue(bean.nestedRead());
        assertTrue(bean.nestedWrite());
    }

    @ApplicationScoped
    static class SimpleApplicationScopedBean {

        @Lock(Type.READ)
        boolean read() {
            return write();
        }

        @Lock(Type.READ)
        boolean nestedRead() {
            return read();
        }

        @Lock(Type.WRITE)
        boolean write() {
            return true;
        }

        @Lock(Type.WRITE)
        boolean nestedWrite() {
            return nestedRead();
        }
    }
}

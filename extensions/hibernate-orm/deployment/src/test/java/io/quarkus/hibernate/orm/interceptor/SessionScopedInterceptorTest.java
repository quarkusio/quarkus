package io.quarkus.hibernate.orm.interceptor;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class SessionScopedInterceptorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyEntity.class, QuarkusEmptyInterceptor.class)
                    .addAsResource(EmptyAsset.INSTANCE, "import.sql")
                    .addAsResource("application-session-scoped-interceptor.properties", "application.properties"));

    @BeforeEach
    public void activateRequestContext() {
        Arc.container().requestContext().activate();
    }

    @Test
    public void testSessionScopedInterceptor() throws Exception {
        // Check if the interceptor is re-instantiate
        Interceptor firstInterceptor = Arc.container().instance(SessionFactory.class).get().getSessionFactoryOptions()
                .getStatelessInterceptorImplementorSupplier().get();
        assertNotNull(firstInterceptor);
        assertInstanceOf(QuarkusEmptyInterceptor.class, firstInterceptor);
        Interceptor secondInterceptor = Arc.container().instance(SessionFactory.class).get().getSessionFactoryOptions()
                .getStatelessInterceptorImplementorSupplier().get();
        assertNotNull(secondInterceptor);
        assertInstanceOf(QuarkusEmptyInterceptor.class, secondInterceptor);
        assertNotSame(firstInterceptor, secondInterceptor);
    }

    @AfterEach
    public void terminateRequestContext() {
        Arc.container().requestContext().terminate();
    }
}

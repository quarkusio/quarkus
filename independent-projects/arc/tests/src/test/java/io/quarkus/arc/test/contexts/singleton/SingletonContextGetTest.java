package io.quarkus.arc.test.contexts.singleton;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.test.ArcTestContainer;

public class SingletonContextGetTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Boom.class);

    @Test
    public void testGet() {
        InjectableContext singletonContext = Arc.container().getActiveContext(Singleton.class);
        assertNotNull(singletonContext);
        List<InjectableContext> singletonContexts = Arc.container().getContexts(Singleton.class);
        assertEquals(1, singletonContexts.size());
        InjectableBean<Boom> boomBean = Arc.container().instance(Boom.class).getBean();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> singletonContext.get(boomBean));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> singletonContext.get(boomBean, new CreationalContextImpl<>(boomBean)));
    }

    @RequestScoped
    public static class Boom {

    }

}

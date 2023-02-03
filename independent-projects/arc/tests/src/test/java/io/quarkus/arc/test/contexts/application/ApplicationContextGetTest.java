package io.quarkus.arc.test.contexts.application;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.test.ArcTestContainer;

public class ApplicationContextGetTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Boom.class);

    @Test
    public void testGet() {
        InjectableContext appContext = Arc.container().getActiveContext(ApplicationScoped.class);
        assertNotNull(appContext);
        List<InjectableContext> appContexts = Arc.container().getContexts(ApplicationScoped.class);
        assertEquals(1, appContexts.size());
        InjectableBean<Boom> boomBean = Arc.container().instance(Boom.class).getBean();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> appContext.get(boomBean));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> appContext.get(boomBean, new CreationalContextImpl<>(boomBean)));
    }

    @Singleton
    public static class Boom {

    }

}

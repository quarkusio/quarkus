package io.quarkus.arc.test.contexts.application;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ApplicationContextGetTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Boom.class);

    @Test
    public void testGet() {
        InjectableContext appContext = Arc.container().getActiveContext(ApplicationScoped.class);
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

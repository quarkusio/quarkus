package io.quarkus.arc.test.contexts.singleton;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SingletonContextGetTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Boom.class);

    @Test
    public void testGet() {
        InjectableContext appContext = Arc.container().getActiveContext(Singleton.class);
        InjectableBean<Boom> boomBean = Arc.container().instance(Boom.class).getBean();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> appContext.get(boomBean));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> appContext.get(boomBean, new CreationalContextImpl<>(boomBean)));
    }

    @RequestScoped
    public static class Boom {

    }

}

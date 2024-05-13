package io.quarkus.rest.client.reactive.beanTypes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that resources processed by RR have bean types equivalent to that of the impl class plus all interfaces in
 * their hierarchy
 */
public class ResourceBeanTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, ClientMock.class, MyBean.class, Alpha.class, Beta.class,
                            Charlie.class, Delta.class));

    @Inject
    MyBean myBean;

    @Test
    void shouldHaveAllInterfaceTypes() {
        // firstly, sanity check
        assertThat(myBean.test()).isEqualTo("hello from " + ClientMock.class);

        // now see what types does the Client bean have - the impl class and all interfaces should be in place
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(Client.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        assertThat(resolvedBean.getScope()).isEqualTo(ApplicationScoped.class);
        assertThat(resolvedBean.getTypes()).contains(Client.class, ClientMock.class, Alpha.class, Beta.class, Charlie.class,
                Delta.class);
    }
}

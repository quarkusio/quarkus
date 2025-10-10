package io.quarkus.it.jpa.util;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyCdiContext {

    public static void checkAvailable(MyCdiContext injected, BeanInstantiator beanInstantiator) {
        assertThat(injected)
                .as("CDI context should be available")
                .isNotNull()
                .returns(true, MyCdiContext::worksProperly);
        assertThat(beanInstantiator).isEqualTo(BeanInstantiator.ARC);
    }

    public static void checkNotAvailable(MyCdiContext injected, BeanInstantiator beanInstantiator) {
        assertThat(injected)
                .as("CDI context should not be available")
                .isNull();
        assertThat(beanInstantiator).isEqualTo(BeanInstantiator.HIBERNATE);
    }

    public boolean worksProperly() {
        return true;
    }
}

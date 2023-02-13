package io.quarkus.it.jpa.entitylistener;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyCdiContext {

    public static void checkAvailable(MyCdiContext injected) {
        assertThat(injected)
                .as("CDI context should be available")
                .isNotNull()
                .returns(true, MyCdiContext::worksProperly);
    }

    public boolean worksProperly() {
        return true;
    }

}

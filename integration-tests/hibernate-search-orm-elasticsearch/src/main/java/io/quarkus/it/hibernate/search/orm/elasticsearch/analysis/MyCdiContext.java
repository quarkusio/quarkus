package io.quarkus.it.hibernate.search.orm.elasticsearch.analysis;

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

    public static void checkNotAvailable(MyCdiContext injected) {
        assertThat(injected)
                .as("CDI context should not be available")
                .isNull();
    }

    public boolean worksProperly() {
        return true;
    }
}

package io.quarkus.arc.test.config.staticinit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class StaticInitBean {

    @ConfigProperty(name = "apfelstrudel")
    String value;

    // bean is instantiated during STATIC_INIT
    void onInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
    }

}

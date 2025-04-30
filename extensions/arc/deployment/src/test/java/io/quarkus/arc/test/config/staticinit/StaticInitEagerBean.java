package io.quarkus.arc.test.config.staticinit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class StaticInitEagerBean {

    @ConfigProperty(name = "apfelstrudel")
    Instance<String> value;

    // bean is instantiated during STATIC_INIT
    void onInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        // this should trigger the failure
        value.get();
    }

}

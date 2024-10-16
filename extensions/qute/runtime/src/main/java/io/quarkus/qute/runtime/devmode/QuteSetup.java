package io.quarkus.qute.runtime.devmode;

import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.arc.Arc;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.qute.Engine;
import io.quarkus.qute.runtime.TemplateProducer;

public class QuteSetup implements HotReplacementSetup {

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        context.consumeNoRestartChanges(new Consumer<Set<String>>() {

            @Override
            public void accept(Set<String> files) {
                // Make sure all templates are reloaded
                Engine engine = Arc.container().instance(Engine.class).get();
                engine.clearTemplates();
                TemplateProducer templateProducer = Arc.container().instance(TemplateProducer.class).get();
                templateProducer.clearInjectedTemplates();
            }
        });
    }

}

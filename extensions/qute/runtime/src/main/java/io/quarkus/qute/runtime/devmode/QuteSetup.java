package io.quarkus.qute.runtime.devmode;

import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
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
                ArcContainer container = Arc.container();
                if (container != null) {
                    InstanceHandle<Engine> engineHandle = container.instance(Engine.class);
                    if (engineHandle.isAvailable()) {
                        engineHandle.get().clearTemplates();
                    }
                    InstanceHandle<TemplateProducer> templateProducerHandle = container.instance(TemplateProducer.class);
                    if (templateProducerHandle.isAvailable()) {
                        templateProducerHandle.get().clearInjectedTemplates();
                    }
                }
            }
        });
    }

}

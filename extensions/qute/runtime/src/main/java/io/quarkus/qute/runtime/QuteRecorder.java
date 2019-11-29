package io.quarkus.qute.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuteRecorder {

    public void initEngine(QuteConfig config, BeanContainer container, List<String> resolverClasses,
            List<String> templatePaths, List<String> tags) {
        EngineProducer producer = container.instance(EngineProducer.class);
        producer.init(config, resolverClasses, templatePaths, tags);
    }

    public void initVariants(BeanContainer container, Map<String, List<String>> variants) {
        VariantTemplateProducer producer = container.instance(VariantTemplateProducer.class);
        producer.init(variants);
    }

}

package io.quarkus.qute.runtime;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.qute.Engine;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuteRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuteRecorder.class);

    public void initEngine(QuteConfig config, BeanContainer container, List<String> resolverClasses,
            List<String> templatePaths, List<String> tags) {
        EngineProducer producer = container.instance(EngineProducer.class);
        producer.init(config, resolverClasses, templatePaths, tags);
    }

    public void initVariants(BeanContainer container, Map<String, List<String>> variants) {
        VariantTemplateProducer producer = container.instance(VariantTemplateProducer.class);
        producer.init(variants);
    }

    public static void clearTemplates(Set<String> paths) {
        EngineProducer engineProducer = Arc.container().instance(EngineProducer.class).get();
        Set<String> pathIds = paths.stream().map(path -> {
            String id = path;
            if (path.startsWith(engineProducer.getTagPath())) {
                // ["META-INF/resources/templates/tags/item.html"] -> ["item.html"]
                id = path.substring(engineProducer.getTagPath().length());
            } else if (path.startsWith(engineProducer.getBasePath())) {
                // ["META-INF/resources/templates/items.html"] -> ["items.html"]
                id = path.substring(engineProducer.getBasePath().length());
            }
            return id;
        }).collect(Collectors.toSet());

        if (pathIds.isEmpty()) {
            return;
        }

        Engine engine = engineProducer.getEngine();
        if (engine != null) {
            engine.removeTemplates(id -> {
                // Exact match or path starts with id, e.g. "items.html" starts with "items"
                boolean remove = pathIds.contains(id) || pathIds.stream().anyMatch(pid -> pid.startsWith(id));
                if (remove) {
                    LOGGER.info("Going to remove the template with id: {}", id);
                }
                return remove;
            });
        }
    }

}

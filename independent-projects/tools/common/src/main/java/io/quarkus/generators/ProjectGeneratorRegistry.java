package io.quarkus.generators;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="claprun@redhat.com">Christophe Laprun</a>
 */
public class ProjectGeneratorRegistry {

    private static final Map<String, ProjectGenerator> generators = new ConcurrentHashMap<>(7);
    private static final ProjectGeneratorRegistry INSTANCE = new ProjectGeneratorRegistry();

    private ProjectGeneratorRegistry() {
        loadGenerators();
    }

    public static ProjectGeneratorRegistry getInstance() {
        return INSTANCE;
    }

    public static ProjectGenerator get(String name) throws NoSuchElementException {
        final ProjectGenerator generator = generators.get(name);
        if (generator == null) {
            throw new NoSuchElementException("Unknown generator: " + name);
        }

        return generator;
    }

    private static void register(ProjectGenerator generator) {
        if (generator != null) {
            generators.put(generator.getName(), generator);
        } else {
            throw new NullPointerException("Cannot register null generator");
        }
    }

    private static void loadGenerators() {
        ServiceLoader<ProjectGenerator> serviceLoader = ServiceLoader.load(ProjectGenerator.class);
        serviceLoader.iterator().forEachRemaining(ProjectGeneratorRegistry::register);
    }
}

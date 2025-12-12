package io.quarkus.deployment.jvm;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * Context for looking up modules in a classloader:
 * encapsulates code dealing with the module layers,
 * and provides a cache for the modules found.
 */
final class ModulesClassloaderContext {

    private final Optional<Module> classloaderUnnamedModule; //The unnamed module for this reference classloader
    private final ModuleLayer currentLayer; //The module layer for this reference classloader
    private final ConcurrentHashMap<String, Optional<Module>> moduleCache = new ConcurrentHashMap<>();

    public ModulesClassloaderContext(final ClassLoader classloader) {
        Module unnamedModule = classloader.getUnnamedModule();
        this.classloaderUnnamedModule = Optional.of(unnamedModule);
        this.currentLayer = currentLayer(unnamedModule);
    }

    public Optional<Module> findModule(final String moduleName) {
        if (ModuleOpenBuildItem.ALL_UNNAMED.equals(moduleName)) {
            return classloaderUnnamedModule;
        }
        return moduleCache.computeIfAbsent(moduleName, this.currentLayer::findModule);
    }

    private static ModuleLayer currentLayer(final Module module) {
        ModuleLayer layer = module.getLayer();
        if (layer == null) {
            return ModuleLayer.boot();
        }
        return layer;
    }

}

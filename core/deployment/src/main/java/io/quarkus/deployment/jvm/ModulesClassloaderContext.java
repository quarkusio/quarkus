package io.quarkus.deployment.jvm;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Context for looking up modules in a classloader:
 * encapsulates code dealing with the module layers,
 * and provides a cache for the modules found.
 * N.B. at this time, to be lenient during evolutions,
 * any module which can't be found by name will resolve
 * to the unnamed module of the reference classloader
 * being passed to the constructor; this can be observed
 * by setting log level to DEBUG of category {@code io.quarkus.deployment.jvm}.
 */
final class ModulesClassloaderContext {

    private final Module classloaderUnnamedModule; //The unnamed module for this reference classloader
    private final ModuleLayer currentLayer; //The module layer for this reference classloader
    private final ConcurrentHashMap<String, Module> moduleCache = new ConcurrentHashMap<>();

    public ModulesClassloaderContext(final ClassLoader classloader) {
        Module unnamedModule = classloader.getUnnamedModule();
        this.classloaderUnnamedModule = unnamedModule;
        this.currentLayer = currentLayer(unnamedModule);
    }

    public Module findModule(final String moduleName) {
        return moduleCache.computeIfAbsent(moduleName, this::findOrFallbackModule);
    }

    private Module findOrFallbackModule(final String moduleName) {
        return currentLayer.findModule(moduleName).orElseGet(() -> {
            JVMDeploymentLogger.logger.debugf(
                    "Module %s not found, falling back to unnamed module", moduleName);
            return classloaderUnnamedModule;
        });
    }

    private static ModuleLayer currentLayer(final Module module) {
        ModuleLayer layer = module.getLayer();
        if (layer == null) {
            return ModuleLayer.boot();
        }
        return layer;
    }

}

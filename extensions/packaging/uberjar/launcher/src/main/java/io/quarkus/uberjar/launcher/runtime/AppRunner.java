package io.quarkus.uberjar.launcher.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.List;

import io.smallrye.common.resource.JarFileResourceLoader;
import io.smallrye.common.resource.Resource;
import io.smallrye.modules.DelegatingModuleLoader;
import io.smallrye.modules.FoundModule;
import io.smallrye.modules.LoadedModule;
import io.smallrye.modules.ModuleDescriptorLoader;
import io.smallrye.modules.ModuleFinder;
import io.smallrye.modules.ModuleLoader;
import io.smallrye.modules.ResourceLoaderOpener;

/**
 * Runner loaded inside the JPMS boot module layer (bootCL) that loads and runs
 * the application from nested JAR resources using SmallRye Modules APIs.
 */
public final class AppRunner {

    private AppRunner() {
    }

    /**
     * Run the application.
     *
     * @param appModuleName the name of the application boot module
     * @param args the command-line arguments
     * @throws Throwable if bootstrap fails
     */
    public static void run(String appModuleName, String[] args) throws Throwable {
        // set TCCL to my loader (the uberjar boot loader) for init
        Thread.currentThread().setContextClassLoader(AppRunner.class.getClassLoader());
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        // Locate the outer UberJar path (classpath tier)
        Path jarPath = Path.of(AppRunner.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        // Open the outer JAR as a JarFileResourceLoader (classpath tier)
        JarFileResourceLoader outerLoader = new JarFileResourceLoader(jarPath);

        // 1. Create a custom ModuleFinder that loads nested STORED JARs directly from outerLoader
        ModuleFinder appFinder = name -> {
            String entryPath = "modules/" + name + ".jar";
            try {
                // Find the nested JAR resource inside the outer JAR (returns a nested ArchiveJarFileResource)
                Resource jarResource = outerLoader.findResource(entryPath);
                if (jarResource == null) {
                    return null;
                }
                // Open the nested JAR resource loader natively (backed by the outer Path, creating standard jar:jar:file URLs)
                ResourceLoaderOpener opener = ResourceLoaderOpener.forJarResource(jarResource);
                return new FoundModule(List.of(opener), ModuleDescriptorLoader.basic());
            } catch (Exception e) {
                throw new RuntimeException("Failed to open nested JAR resource for module: " + name, e);
            }
        };

        // 2. Compose the module loaders
        // this will generally return {@code null} unless we're in a special environment of some sort
        ModuleLoader parentLoader = ModuleLoader.ofClass(AppRunner.class);
        if (parentLoader == null) {
            // otherwise, we'll wire in the layer from the JDK
            ModuleLayer layer = AppRunner.class.getModule().getLayer();
            if (layer == null) {
                layer = ModuleLayer.boot();
            }
            parentLoader = ModuleLoader.forLayer("launcher", layer);
        }
        ModuleLoader customLoader = new DelegatingModuleLoader("app", appFinder, parentLoader);

        // 3. Load the main application module, set TCCL, and invoke the main method
        LoadedModule loadedAppModule = customLoader.loadModule(appModuleName);
        if (loadedAppModule == null) {
            throw new IllegalStateException("Failed to load application module: " + appModuleName);
        }

        // set TCCL to the final app
        Thread.currentThread().setContextClassLoader(loadedAppModule.classLoader());

        String mainClassName = loadedAppModule.module().getDescriptor().mainClass()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application module " + appModuleName + " does not have a defined main class"));

        Class<?> mainClass = Class.forName(mainClassName, true, loadedAppModule.classLoader());
        MethodHandle mainMethodHandle = MethodHandles.publicLookup()
                .findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));

        // Launch!
        mainMethodHandle.invokeExact(args);
    }
}

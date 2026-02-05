package io.quarkus.jlink.launcher;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.List;

import io.smallrye.modules.LoadedModule;
import io.smallrye.modules.ModuleFinder;
import io.smallrye.modules.ModuleLoader;

public final class JLinkAppLauncher {
    private JLinkAppLauncher() {
    }

    /**
     * Launch the application.
     *
     * @param args the application arguments
     */
    public static void run(String appModule, String[] args) {
        Module myModule = JLinkAppLauncher.class.getModule();
        if (myModule == null) {
            throw new IllegalStateException("Must launch jlink image in module mode only");
        }
        ModuleLayer myLayer = myModule.getLayer();
        if (myLayer == null) {
            throw new IllegalStateException("Module of jlink image launcher is not in a module layer");
        }

        @SuppressWarnings("resource")
        ModuleLoader base = ModuleLoader.forLayer("base", myLayer);

        // try to ascertain our own path
        String cmd = ProcessHandle.current().info().command()
                .orElseThrow(() -> new IllegalStateException("Cannot determine image path (ProcessHandle)"));
        Path cmdPath = Path.of(cmd);
        Path binPath = cmdPath.getParent();
        if (binPath == null || !binPath.getFileName().toString().equals("bin")) {
            throw new IllegalStateException("Cannot determine image path (bin path)");
        }
        Path imagePath = binPath.getParent();
        if (imagePath == null) {
            throw new IllegalStateException("Cannot determine image path (image path)");
        }

        Path libPath = imagePath.resolve("lib").resolve("quarkus");

        // create a layer for our dynamic modules
        ModuleLoader dyn = new ModuleLoader("dyn", ModuleFinder.fromFileSystem(List.of(libPath))) {
            public LoadedModule loadModule(final String moduleName) {
                LoadedModule found = base.loadModule(moduleName);
                if (found == null) {
                    found = super.loadModule(moduleName);
                }
                return found;
            }
        };

        // load the application
        LoadedModule loadedModule = dyn.loadModule(appModule);
        if (loadedModule == null) {
            throw new IllegalArgumentException("Application module " + appModule + " not found");
        }

        // set up TCCL; todo: stop relying on this
        Thread.currentThread().setContextClassLoader(loadedModule.classLoader());

        // find the main class
        String mainClassName = loadedModule.module()
                .getDescriptor()
                .mainClass()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application module " + appModule + " does not have a defined main class"));
        Class<?> mainClass;
        try {
            mainClass = Class.forName(mainClassName, true, loadedModule.classLoader());
        } catch (ClassNotFoundException e) {
            throw sneak(e);
        }

        // find the main method
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodHandle mainMethodHandle;
        try {
            mainMethodHandle = publicLookup.findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw sneak(e);
        }

        // launch the application
        try {
            //noinspection ConfusingArgumentToVarargsMethod
            mainMethodHandle.invokeExact(args);
        } catch (Throwable e) {
            throw sneak(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneak(Throwable t) throws E {
        throw (E) t;
    }
}

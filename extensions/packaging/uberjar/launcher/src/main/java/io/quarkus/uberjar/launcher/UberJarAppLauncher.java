package io.quarkus.uberjar.launcher;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodHandles.publicLookup;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.smallrye.modules.boot.BootClassLoader;

/**
 * Custom entry point for the modular UberJar that bootstraps the minimal JPMS layer
 * (logging, launcher, smallrye-modules runtime) and hands off execution reflectively
 * to the AppRunner class inside the JPMS boot module layer (bootCL).
 * <p>
 * This class MUST NOT statically link (import or reference) any classes inside the
 * smallrye-modules runtime to prevent linkage NoClassDefFoundError failures on the
 * System Classloader.
 */
public final class UberJarAppLauncher {

    private UberJarAppLauncher() {
    }

    /**
     * Main entry point.
     *
     * @param args the original command-line arguments
     * @throws Throwable if bootstrap fails
     */
    public static void main(String[] args) throws Throwable {
        // 1. Locate the composite JAR from our own code source
        CodeSource codeSource = UberJarAppLauncher.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException("Cannot determine UberJar code source location");
        }
        Path jarPath = Path.of(codeSource.getLocation().toURI());

        // 2. Memory-map the entire file
        ByteBuffer compositeBuffer;
        try (FileChannel channel = FileChannel.open(jarPath, StandardOpenOption.READ)) {
            compositeBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }

        // 3. Load the synthesized JarIndexImpl from the system class loader
        JarIndexImpl jarIndex = new JarIndexImpl();

        // 4. Create boot class loader (reads boot module descriptors)
        BootClassLoader bootCL = new BootClassLoader(compositeBuffer, jarIndex, UberJarAppLauncher.class.getProtectionDomain());

        // 5. Build the minimal JPMS layer for boot modules (io.smallrye.modules, logging, launcher)
        Map<String, ModuleReference> moduleRefs = buildModuleReferences(compositeBuffer, jarIndex);
        ModuleFinder finder = new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                return Optional.ofNullable(moduleRefs.get(name));
            }

            @Override
            public Set<ModuleReference> findAll() {
                return Set.copyOf(moduleRefs.values());
            }
        };

        Set<String> roots = Set.copyOf(jarIndex.moduleNames());
        Configuration cf = ModuleLayer.boot().configuration().resolve(
                finder, ModuleFinder.of(), roots);

        ModuleLayer.Controller controller = ModuleLayer.defineModules(
                cf, List.of(ModuleLayer.boot()), name -> bootCL);
        ModuleLayer infraLayer = controller.layer();

        // 6. Transfer the jdk.internal.module export to the infrastructure module
        Module infraModule = infraLayer.findModule("io.smallrye.modules")
                .orElseThrow(() -> new IllegalStateException("io.smallrye.modules not found in infrastructure layer"));
        transferInternalExport(infraModule);

        // 7. Read the name of the main application module to boot
        String appModuleName;
        try (InputStream is = UberJarAppLauncher.class.getResourceAsStream("/META-INF/uberjar-boot-module.txt")) {
            if (is == null) {
                throw new IllegalStateException("Failed to find boot module metadata: /META-INF/uberjar-boot-module.txt");
            }
            appModuleName = new String(is.readAllBytes(), StandardCharsets.UTF_8).strip();
        }

        // 8. Find and invoke io.quarkus.uberjar.launcher.runtime.AppRunner reflectively from bootCL
        Module launcherModule = infraLayer.findModule("io.quarkus.uberjar.launcher")
                .orElseThrow(() -> new IllegalStateException("io.quarkus.uberjar.launcher not found in infrastructure layer"));

        Class<?> runnerClass = Class.forName(launcherModule, "io.quarkus.uberjar.launcher.runtime.AppRunner");
        if (runnerClass == null) {
            throw new ClassNotFoundException("io.quarkus.uberjar.launcher.runtime.AppRunner not found in infrastructure layer");
        }

        MethodHandle runHandle = publicLookup().findStatic(runnerClass, "run",
                MethodType.methodType(void.class, String.class, String[].class));

        // Launch!
        runHandle.invokeExact(appModuleName, args);
    }

    private static Map<String, ModuleReference> buildModuleReferences(ByteBuffer buffer, JarIndexImpl index) {
        HashMap<String, ModuleReference> refs = new HashMap<>();
        for (String moduleName : index.moduleNames()) {
            long offset = index.classOffset(moduleName, "module-info");
            long size = index.classSize(moduleName, "module-info");
            ByteBuffer descriptorBytes = buffer.slice((int) offset, (int) size);
            ModuleDescriptor descriptor = ModuleDescriptor.read(descriptorBytes);
            ModuleReference ref = new ModuleReference(descriptor, null) {
                @Override
                public ModuleReader open() {
                    return new ModuleReader() {
                        @Override
                        public Optional<URI> find(String name) {
                            return Optional.empty();
                        }

                        @Override
                        public Stream<String> list() {
                            return Stream.empty();
                        }

                        @Override
                        public void close() {
                        }
                    };
                }
            };
            refs.put(moduleName, ref);
        }
        return refs;
    }

    private static void transferInternalExport(Module infraModule) {
        try {
            @SuppressWarnings("Java9ReflectionClassVisibility")
            Class<?> modulesClass = Class.forName("jdk.internal.module.Modules", true, null);
            MethodHandle addExports = lookup().findStatic(modulesClass, "addExports",
                    MethodType.methodType(void.class, Module.class, String.class, Module.class));
            Module javaBase = Object.class.getModule();
            addExports.invokeExact(javaBase, "jdk.internal.module", infraModule);
        } catch (Throwable e) {
            Module self = UberJarAppLauncher.class.getModule();
            String target = self.isNamed() ? self.getName() : "ALL-UNNAMED";
            throw new IllegalStateException(
                    "Failed to transfer jdk.internal.module export — use: --add-exports java.base/jdk.internal.module="
                            + target,
                    e);
        }
    }
}

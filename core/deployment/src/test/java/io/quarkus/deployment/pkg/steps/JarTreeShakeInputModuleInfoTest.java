package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.constant.ClassDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.attribute.ModuleAttribute;
import io.smallrye.classfile.extras.constant.ModuleDesc;

/**
 * Tests for the collection of service providers declared in module descriptors.
 */
class JarTreeShakeInputModuleInfoTest {

    private static final String SERVICE = "org.acme.NativeProvider";

    @Test
    void moduleInfoEntries() {
        assertThat(JarTreeShakeInput.isModuleInfoEntry("module-info.class")).isTrue();
        assertThat(JarTreeShakeInput.isModuleInfoEntry("META-INF/versions/9/module-info.class")).isTrue();
        assertThat(JarTreeShakeInput.isModuleInfoEntry("META-INF/versions/9/org/acme/module-info.class")).isFalse();
        assertThat(JarTreeShakeInput.isModuleInfoEntry("org/acme/module-info.class")).isFalse();
        assertThat(JarTreeShakeInput.isModuleInfoEntry("org/acme/Foo.class")).isFalse();
    }

    @Test
    void providesAreCollected(@TempDir Path dir) throws IOException {
        Path moduleInfo = writeModuleInfo(dir.resolve("module-info.class"), "org.acme.natives.LinuxNativeProvider");
        Map<String, Set<String>> serviceProviders = new HashMap<>();
        // a provider of the same service already collected from a META-INF/services file
        serviceProviders.put(SERVICE, new HashSet<>(Set.of("org.acme.FromServicesFile")));

        JarTreeShakeInput.parseModuleInfoProviders(Files.readAllBytes(moduleInfo), serviceProviders);

        assertThat(serviceProviders).containsOnly(Map.entry(SERVICE,
                Set.of("org.acme.FromServicesFile", "org.acme.natives.LinuxNativeProvider")));
    }

    @Test
    void overriddenLowerVersionDescriptorsAreIgnored(@TempDir Path dir) throws IOException {
        Map<String, Set<String>> serviceProviders = new HashMap<>();
        var selector = new JarTreeShakeInput.ModuleDescriptorSelector(17, serviceProviders);
        selector.add("module-info.class", writeModuleInfo(dir.resolve("root.class"), "org.acme.RootProvider"));
        selector.add("META-INF/versions/9/module-info.class",
                writeModuleInfo(dir.resolve("v9.class"), "org.acme.Java9Provider"));
        selector.add("META-INF/versions/11/module-info.class",
                writeModuleInfo(dir.resolve("v11.class"), "org.acme.Java11Provider"));
        selector.add("META-INF/versions/21/module-info.class",
                writeModuleInfo(dir.resolve("v21.class"), "org.acme.Java21Provider"));
        selector.add("META-INF/versions/x/module-info.class",
                writeModuleInfo(dir.resolve("vx.class"), "org.acme.InvalidVersionProvider"));

        selector.complete();

        // the highest version not newer than the app's one, plus the newer ones that a newer JVM may read
        assertThat(serviceProviders).containsOnly(Map.entry(SERVICE,
                Set.of("org.acme.Java11Provider", "org.acme.Java21Provider")));
    }

    @Test
    void rootDescriptorIsSelectedWithoutVersionedOnes(@TempDir Path dir) throws IOException {
        Map<String, Set<String>> serviceProviders = new HashMap<>();
        var selector = new JarTreeShakeInput.ModuleDescriptorSelector(17, serviceProviders);
        selector.add("module-info.class", writeModuleInfo(dir.resolve("root.class"), "org.acme.RootProvider"));

        selector.complete();

        assertThat(serviceProviders).containsOnly(Map.entry(SERVICE, Set.of("org.acme.RootProvider")));
    }

    private static Path writeModuleInfo(Path file, String provider) throws IOException {
        Files.write(file, ClassFile.of().buildModule(ModuleAttribute.of(ModuleDesc.of("org.acme.natives"),
                module -> module
                        .requires(ModuleDesc.of("java.base"), ClassFile.ACC_MANDATED, null)
                        .provides(ClassDesc.of(SERVICE), ClassDesc.of(provider)))));
        return file;
    }
}

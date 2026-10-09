package io.quarkus.netty.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.smallrye.config.SmallRyeConfigBuilder;

class CleanerJava24LinkerInitTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void onlyJvmNeedsStaticInitialization(boolean nativeImage) throws Exception {
        NativeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NativeConfig.class)
                .withDefaultValue("quarkus.native.enabled", Boolean.toString(nativeImage))
                .build().getConfigMapping(NativeConfig.class);
        List<BytecodeTransformerBuildItem> transformations = new ArrayList<>();
        new NettyProcessor().transformCleanerJava24Linker(
                CompiledJavaVersionBuildItem.fromMajorJavaVersion(nativeImage ? 65 : 69), config, transformations::add);

        String className = "io.netty.util.internal.CleanerJava24Linker";
        BytecodeTransformerBuildItem transformation = transformations.stream()
                .filter(item -> item.getClassToTransform().equals(className)).findFirst().orElseThrow();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class")) {
            new ClassReader(input).accept(transformation.getVisitorFunction().apply(className, writer), 0);
        }
        ClassNode transformed = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(transformed, 0);
        if (nativeImage) {
            assertThat(transformed.methods).extracting(method -> method.name)
                    .as("Native allocation must not require cleaner initialization").doesNotContain("<clinit>");
            return;
        }
        List<String> linkerCalls = new ArrayList<>();
        for (var method : transformed.methods) {
            if (method.name.equals("<clinit>")) {
                for (var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call && call.owner.equals("java/lang/foreign/Linker")) {
                        linkerCalls.add(call.name);
                    }
                }
            }
        }
        assertThat(linkerCalls).containsExactly("nativeLinker", "defaultLookup", "downcallHandle", "downcallHandle");
    }
}

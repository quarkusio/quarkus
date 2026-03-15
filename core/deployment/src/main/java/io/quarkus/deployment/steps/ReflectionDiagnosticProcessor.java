package io.quarkus.deployment.steps;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * writes a list of all reflective classes to META-INF, if the quarkus.debug.reflection system property is set
 * <p>
 * This is not really user facing config for now, which is why it is just a system property instead of using the standard
 * config mechanism
 */
public class ReflectionDiagnosticProcessor {

    @BuildStep(onlyIf = { NativeOrNativeSourcesBuild.class, DebugReflectionEnabled.class })
    public List<GeneratedResourceBuildItem> writeReflectionData(
            List<ReflectiveClassBuildItem> classes,
            List<ReflectiveMethodBuildItem> methods,
            List<ReflectiveFieldBuildItem> fields) {
        String classNames = classes.stream()
                .map(ReflectiveClassBuildItem::getClassNames)
                .flatMap(Collection::stream)
                .sorted()
                .distinct()
                .collect(Collectors.joining("\n", "", "\n"));
        String methodNames = methods.stream()
                .map(m -> m.getDeclaringClass() + "#" + m.getName() + "(" + String.join(",", m.getParams()) + ")")
                .sorted()
                .distinct()
                .collect(Collectors.joining("\n", "", "\n"));
        String fieldsNames = fields.stream()
                .map(m -> m.getDeclaringClass() + "#" + m.getName())
                .sorted()
                .distinct()
                .collect(Collectors.joining("\n", "", "\n"));
        return Arrays.asList(
                new GeneratedResourceBuildItem("META-INF/reflective-classes.txt",
                        classNames.getBytes(StandardCharsets.UTF_8)),
                new GeneratedResourceBuildItem("META-INF/reflective-methods.txt",
                        methodNames.getBytes(StandardCharsets.UTF_8)),
                new GeneratedResourceBuildItem("META-INF/reflective-fields.txt",
                        fieldsNames.getBytes(StandardCharsets.UTF_8)));
    }

    public static class DebugReflectionEnabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return Boolean.getBoolean("quarkus.debug.reflection");
        }
    }
}

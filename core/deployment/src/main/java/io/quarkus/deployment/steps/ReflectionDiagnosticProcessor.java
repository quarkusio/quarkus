package io.quarkus.deployment.steps;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

/**
 * writes a list of all reflective classes to META-INF, if the quarkus.debug.reflection system property is set
 * <p>
 * This is not really user facing config for now, which is why it is just a system property instead of using the standard
 * config mechanism
 */
public class ReflectionDiagnosticProcessor {

    @BuildStep
    public GeneratedResourceBuildItem writeReflectionData(List<ReflectiveClassBuildItem> classes) {
        if (Boolean.getBoolean("quarkus.debug.reflection")) {
            StringBuilder sb = new StringBuilder();
            Set<String> seen = new HashSet<>();
            for (ReflectiveClassBuildItem i : classes) {
                for (String j : i.getClassNames()) {
                    if (seen.add(j)) {
                        sb.append(j);
                        sb.append("\n");
                    }
                }
            }
            return new GeneratedResourceBuildItem("META-INF/reflective-classes.txt",
                    sb.toString().getBytes(StandardCharsets.UTF_8));
        } else {
            return null;
        }
    }

}

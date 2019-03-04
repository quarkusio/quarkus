package io.quarkus.deployment.steps;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

/**
 * writes a list of all reflective classes to META-INF
 */
public class ReflectionDiagnosticProcessor {

    @BuildStep
    public GeneratedResourceBuildItem writeReflectionData(List<ReflectiveClassBuildItem> classes) {
        StringBuilder sb = new StringBuilder();
        for (ReflectiveClassBuildItem i : classes) {
            for (String j : i.getClassNames()) {
                sb.append(j);
                sb.append("\n");
            }
        }
        return new GeneratedResourceBuildItem("META-INF/reflective-classes.txt",
                sb.toString().getBytes(StandardCharsets.UTF_8));
    }

}

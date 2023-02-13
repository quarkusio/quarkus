package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.builder.Json;
import io.quarkus.builder.Json.JsonArrayBuilder;
import io.quarkus.builder.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.LambdaCapturingTypeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class NativeImageSerializationConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateSerializationConfig(BuildProducer<GeneratedResourceBuildItem> serializationConfig,
            List<ReflectiveClassBuildItem> reflectiveClassBuildItems,
            List<LambdaCapturingTypeBuildItem> lambdaCapturingTypeBuildItems) {

        final Set<String> serializableClasses = new HashSet<>();
        for (ReflectiveClassBuildItem i : reflectiveClassBuildItems) {
            if (i.isSerialization()) {
                String[] classNames = i.getClassNames().toArray(new String[0]);
                Collections.addAll(serializableClasses, classNames);
            }
        }

        JsonObjectBuilder root = Json.object();
        JsonArrayBuilder types = Json.array();
        for (String serializableClass : serializableClasses) {
            types.add(Json.object().put("name", serializableClass));
        }
        root.put("types", types);

        JsonArrayBuilder lambdaCapturingTypes = Json.array();
        if (!lambdaCapturingTypeBuildItems.isEmpty()) {
            for (LambdaCapturingTypeBuildItem i : lambdaCapturingTypeBuildItems) {
                lambdaCapturingTypes.add(Json.object().put("name", i.getClassName()));
            }
        }
        root.put("lambdaCapturingTypes", lambdaCapturingTypes);

        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            serializationConfig.produce(new GeneratedResourceBuildItem("META-INF/native-image/serialization-config.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

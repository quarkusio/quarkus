package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonArrayBuilder;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.LambdaCapturingTypeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * Schema used:
 * https://github.com/graalvm/graalvm-community-jdk25u/blob/master/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.2.0.json
 *
 * See https://www.graalvm.org/latest/reference-manual/native-image/metadata/#serialization-metadata-registration-in-code
 */
public class NativeImageSerializationConfigStep {

    private static final Logger log = Logger.getLogger(NativeImageSerializationConfigStep.class);

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateSerializationConfig(BuildProducer<GeneratedResourceBuildItem> serializationConfig,
            List<ReflectiveClassBuildItem> reflectiveClassBuildItems,
            List<LambdaCapturingTypeBuildItem> lambdaCapturingTypeBuildItems) {

        final Set<String> serializableClasses = new HashSet<>();

        for (ReflectiveClassBuildItem i : reflectiveClassBuildItems) {
            if (i.isSerialization()) {
                final String[] classNames = i.getClassNames().toArray(new String[0]);
                Collections.addAll(serializableClasses, classNames);
            }
        }

        if (!lambdaCapturingTypeBuildItems.isEmpty()) {
            final List<String> ignoredLambdas = new ArrayList<>();
            for (LambdaCapturingTypeBuildItem i : lambdaCapturingTypeBuildItems) {
                ignoredLambdas.add(i.getClassName());
            }
            log.errorf(
                    "GraalVM reachability-metadata does not support legacy @RegisterForReflection(serialization = true, lambdaCapturingTypes.... "
                            + "Use ObjectInputFilter programatically to register lambda serialization "
                            + "(e.g., ObjectInputFilter.Config.createFilter(\"EnclosingClass$$Lambda*;\")). " +
                            " As used in Quarkus' integration-tests/native-image-annotations/src/main/java/io/quarkus/it/nat/annotation/NativeImageAnnotationsResource.java. "
                            + "Ignoring these: %s",
                    ignoredLambdas);
        }

        if (serializableClasses.isEmpty()) {
            return;
        }

        final JsonArrayBuilder reflectionArray = Json.array();
        for (String serializableClass : serializableClasses) {
            reflectionArray.add(Json.object()
                    .put("type", serializableClass)
                    .put("serializable", true));
        }

        final JsonObjectBuilder root = Json.object().put("reflection", reflectionArray);

        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            serializationConfig.produce(new GeneratedResourceBuildItem(
                    "META-INF/native-image/serialization/reachability-metadata.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

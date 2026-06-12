package io.quarkus.deployment.steps;

import static io.quarkus.deployment.steps.NativeImageFFMConfigStep.isGraalVm25OrNewer;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonArrayBuilder;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.LambdaReflectionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * @formatter:off
 * Schema used:
 * <a href="https://github.com/graalvm/graalvm-community-jdk25u/blob/master/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.2.0.json">reachability-metadata-schema-v1.2.0.json</a>
 * <p>
 * See <a href="https://www.graalvm.org/latest/reference-manual/native-image/metadata/#serialization-metadata-registration-in-code">serialization-metadata</a>
 * Notes on proper testing: At least integration-tests modules main and native-image-annotations.
 * @formatter:on
 */
public class NativeImageSerializationConfigStep {

    private static final String JAVA_IO_SERIALIZABLE = "java.io.Serializable";

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateSerializationConfig(BuildProducer<GeneratedResourceBuildItem> serializationConfig,
            List<ReflectiveClassBuildItem> reflectiveClassBuildItems,
            List<LambdaReflectionBuildItem> lambdaReflectionBuildItems,
            NativeImageRunnerBuildItem nativeImageRunnerBuildItem) {

        final Set<String> serializableClasses = new HashSet<>();
        final Map<String, Set<LambdaReflectionBuildItem>> lambdasByDeclaringClass = new HashMap<>();

        for (ReflectiveClassBuildItem i : reflectiveClassBuildItems) {
            if (i.isSerialization()) {
                Collections.addAll(serializableClasses, i.getClassNames().toArray(new String[0]));
            }
        }

        for (LambdaReflectionBuildItem lambda : lambdaReflectionBuildItems) {
            lambdasByDeclaringClass.computeIfAbsent(lambda.getDeclaringClass(), k -> new HashSet<>()).add(lambda);
        }

        if (serializableClasses.isEmpty() && lambdasByDeclaringClass.isEmpty()) {
            return;
        }

        final boolean isGraalVm25OrNewer = isGraalVm25OrNewer(nativeImageRunnerBuildItem);

        if (isGraalVm25OrNewer) {
            final JsonObjectBuilder root = Json.object();
            final JsonArrayBuilder reflectionArray = buildMandrel25ReflectionArray(serializableClasses,
                    lambdasByDeclaringClass);
            if (!reflectionArray.isEmpty()) {
                root.put("reflection", reflectionArray);
            }
            try (StringWriter writer = new StringWriter()) {
                root.appendTo(writer);
                serializationConfig.produce(new GeneratedResourceBuildItem(
                        "META-INF/native-image/serialization/reachability-metadata.json",
                        writer.toString().getBytes(StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // GraalVM/Mandrel 21 vs 25+ reachability-metadata.json ParserConfigurationAdapter incompatibility.
            // TODO: Remove when we drop GraalVM/Mandrel for JDK 21
            final JsonArrayBuilder reflectionArray = buildMandrel21ReflectionArray(lambdasByDeclaringClass);
            if (!reflectionArray.isEmpty()) {
                final JsonObjectBuilder root = Json.object().put("reflection", reflectionArray);
                try (StringWriter writer = new StringWriter()) {
                    root.appendTo(writer);
                    serializationConfig.produce(new GeneratedResourceBuildItem(
                            "META-INF/native-image/serialization/reachability-metadata.json",
                            writer.toString().getBytes(StandardCharsets.UTF_8)));
                    serializationConfig.produce(new GeneratedResourceBuildItem(
                            "META-INF/native-image/serialization/native-image.properties",
                            "Args = -H:+UnlockExperimentalVMOptions -H:ConfigurationResourceRoots=META-INF/native-image/serialization/ -H:-UnlockExperimentalVMOptions\n"
                                    .getBytes(StandardCharsets.UTF_8)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            final JsonObjectBuilder serializationObj = buildMandrel21SerializationObject(serializableClasses,
                    lambdasByDeclaringClass);
            if (!serializationObj.isEmpty()) {
                try (StringWriter writer = new StringWriter()) {
                    serializationObj.appendTo(writer);
                    serializationConfig.produce(new GeneratedResourceBuildItem(
                            "META-INF/native-image/serialization-config.json",
                            writer.toString().getBytes(StandardCharsets.UTF_8)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private JsonArrayBuilder buildMandrel25ReflectionArray(Set<String> serializableClasses,
            Map<String, Set<LambdaReflectionBuildItem>> lambdasByDeclaringClass) {
        final JsonArrayBuilder reflectionArray = Json.array();
        // regular serializable classes
        for (String serializableClass : serializableClasses) {
            reflectionArray.add(Json.object()
                    .put("type", serializableClass)
                    .put("serializable", true));
        }
        // lambda metadata for each declaring class
        for (Map.Entry<String, Set<LambdaReflectionBuildItem>> entry : lambdasByDeclaringClass.entrySet()) {
            final String declaringClass = entry.getKey();
            // $deserializeLambda$ method registration for the declaring class
            // Why? https://github.com/oracle/graal/issues/13665
            reflectionArray.add(Json.object()
                    .put("type", declaringClass)
                    .put("methods", Json.array()
                            .add(Json.object()
                                    .put("name", "$deserializeLambda$")
                                    .put("parameterTypes", Json.array()
                                            .add("java.lang.invoke.SerializedLambda")))));
            // add lambda descriptor for each lambda
            for (LambdaReflectionBuildItem lambda : entry.getValue()) {
                final JsonObjectBuilder lambdaObj = Json.object();
                final JsonObjectBuilder lambdaDescriptor = Json.object();
                lambdaDescriptor.put("declaringClass", lambda.getDeclaringClass());
                if (lambda.getDeclaringMethod() != null && !lambda.getDeclaringMethod().isEmpty()) {
                    final JsonObjectBuilder declaringMethodObj = Json.object();
                    declaringMethodObj.put("name", lambda.getDeclaringMethod());
                    final JsonArrayBuilder paramTypesArray = Json.array();
                    paramTypesArray.addAll(Arrays.asList(lambda.getParameterTypes()));
                    declaringMethodObj.put("parameterTypes", paramTypesArray);
                    lambdaDescriptor.put("declaringMethod", declaringMethodObj);
                }
                final Set<String> interfaces = new HashSet<>(Arrays.asList(lambda.getInterfaces()));
                interfaces.add(JAVA_IO_SERIALIZABLE);
                final JsonArrayBuilder interfacesArray = Json.array();
                interfacesArray.addAll(interfaces);
                lambdaDescriptor.put("interfaces", interfacesArray);
                lambdaObj.put("lambda", lambdaDescriptor);
                reflectionArray.add(Json.object()
                        .put("type", lambdaObj)
                        .put("allDeclaredFields", true)
                        .put("allDeclaredMethods", true));
            }
        }
        return reflectionArray;
    }

    private JsonArrayBuilder buildMandrel21ReflectionArray(
            Map<String, Set<LambdaReflectionBuildItem>> lambdasByDeclaringClass) {
        final JsonArrayBuilder reflectionArray = Json.array();
        for (String declaringClass : lambdasByDeclaringClass.keySet()) {
            reflectionArray.add(Json.object()
                    .put("type", declaringClass)
                    .put("methods", Json.array()
                            .add(Json.object()
                                    .put("name", "$deserializeLambda$")
                                    .put("parameterTypes", Json.array()
                                            .add("java.lang.invoke.SerializedLambda")))));
        }
        return reflectionArray;
    }

    /**
     * Legacy, drop when we drop Mandrel/GraalVM for JDK 21.
     */
    private JsonObjectBuilder buildMandrel21SerializationObject(Set<String> serializableClasses,
            Map<String, Set<LambdaReflectionBuildItem>> lambdasByDeclaringClass) {
        final JsonObjectBuilder serializationObj = Json.object();
        if (!serializableClasses.isEmpty()) {
            final JsonArrayBuilder typesArray = Json.array();
            for (String serializableClass : serializableClasses) {
                typesArray.add(Json.object().put("name", serializableClass));
            }
            serializationObj.put("types", typesArray);
        }
        if (!lambdasByDeclaringClass.isEmpty()) {
            final JsonArrayBuilder lambdaArray = Json.array();
            for (String declaringClass : lambdasByDeclaringClass.keySet()) {
                lambdaArray.add(Json.object().put("name", declaringClass));
            }
            serializationObj.put("lambdaCapturingTypes", lambdaArray);
        }
        return serializationObj;
    }
}

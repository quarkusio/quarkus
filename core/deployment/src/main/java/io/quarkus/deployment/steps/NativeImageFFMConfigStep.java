package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonArrayBuilder;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.FfmDowncallBuildItem;
import io.quarkus.deployment.builditem.nativeimage.FfmUpcallBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * @formatter:off
 * Creates reachability-metadata.json with:
 * <a href="https://www.graalvm.org/latest/reference-manual/native-image/native-code-interoperability/ffm-api/#registering-foreign-calls">Foreign calls</a>
 * <a href="https://www.graalvm.org/latest/reference-manual/native-image/metadata/#foreign-function-and-memory-api">FFM/FFI config</a>
 * Schema used:
 * <a href="https://github.com/graalvm/graalvm-community-jdk25u/blob/master/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.2.0.json">reachability-metadata-schema-v1.2.0.json</a>
 * Notes on proper testing: At least integration-tests module awt.
 * @formatter:on
 */
public class NativeImageFFMConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateFfmConfig(BuildProducer<GeneratedResourceBuildItem> reachabilityMetadata,
            List<FfmDowncallBuildItem> downcalls,
            List<FfmUpcallBuildItem> upcalls) {
        if (downcalls.isEmpty() && upcalls.isEmpty()) {
            return;
        }
        final JsonObjectBuilder foreignJson = Json.object();
        if (!downcalls.isEmpty()) {
            final JsonArrayBuilder downcallsArray = Json.array();
            downcalls.stream().distinct().forEach(downcall -> {
                final JsonObjectBuilder dcb = Json.object();
                dcb.put("returnType", downcall.getReturnType());
                final JsonArrayBuilder paramsArray = Json.array();
                paramsArray.addAll(downcall.getParameterTypes());
                dcb.put("parameterTypes", paramsArray);
                if (downcall.hasOptions()) {
                    final JsonObjectBuilder options = Json.object();
                    if (downcall.isCaptureCallState()) {
                        options.put("captureCallState", true);
                    }
                    if (downcall.getFirstVariadicArg() >= 0) {
                        options.put("firstVariadicArg", downcall.getFirstVariadicArg());
                    }
                    if (downcall.getCritical() != null) {
                        final JsonObjectBuilder criticalObj = Json.object();
                        criticalObj.put("allowHeapAccess", downcall.getCritical().allowHeapAccess());
                        options.put("critical", criticalObj);
                    }
                    dcb.put("options", options);
                }
                downcallsArray.add(dcb);
            });
            foreignJson.put("downcalls", downcallsArray);
        }
        if (!upcalls.isEmpty()) {
            final JsonArrayBuilder upcallsArray = Json.array();
            upcalls.stream().distinct().forEach(upcall -> {
                final JsonObjectBuilder ucb = Json.object();
                ucb.put("returnType", upcall.getReturnType());
                final JsonArrayBuilder paramsArray = Json.array();
                paramsArray.addAll(upcall.getParameterTypes());
                ucb.put("parameterTypes", paramsArray);
                upcallsArray.add(ucb);
            });
            foreignJson.put("upcalls", upcallsArray);
        }
        final JsonObjectBuilder root = Json.object();
        root.put("foreign", foreignJson);
        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            // The nested location seems important to the native-image metadata lookup.
            reachabilityMetadata.produce(new GeneratedResourceBuildItem(
                    /*
                     * Despite the doc [1] stating:
                     * "located in any of the classpath entries at META-INF/native-image/<group.Id>\/<artifactId>\/."
                     * it is fine both leaving it in `META-INF/native-image` and nesting it in an arbitrary dir.
                     * [1] https://www.graalvm.org/latest/reference-manual/native-image/metadata/#specifying-metadata-with-json
                     */
                    "META-INF/native-image/foreign/reachability-metadata.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

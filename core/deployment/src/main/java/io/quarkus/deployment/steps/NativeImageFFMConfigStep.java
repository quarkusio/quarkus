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

/**
 * Creates reachability-metadata.json with:
 * https://www.graalvm.org/latest/reference-manual/native-image/native-code-interoperability/ffm-api/#registering-foreign-calls
 * https://www.graalvm.org/latest/reference-manual/native-image/metadata/#foreign-function-and-memory-api
 *
 * It does not handle anything else, i.e. it does not implement
 * https://github.com/quarkusio/quarkus/issues/41016
 */
public class NativeImageFFMConfigStep {

    @BuildStep
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

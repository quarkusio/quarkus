package io.quarkus.deployment.steps;

import static io.quarkus.deployment.steps.NativeImageFFMConfigStep.isGraalVm25OrNewer;

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
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * Schema used:
 * https://github.com/graalvm/graalvm-community-jdk25u/blob/master/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.2.0.json
 */
public class NativeImageProxyConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateProxyConfig(BuildProducer<GeneratedResourceBuildItem> proxyConfig,
            List<NativeImageProxyDefinitionBuildItem> proxies,
            NativeImageRunnerBuildItem nativeImageRunnerBuildItem) {

        if (proxies.isEmpty()) {
            return;
        }

        try (StringWriter writer = new StringWriter()) {
            final JsonArrayBuilder reflectionArray = Json.array();
            for (NativeImageProxyDefinitionBuildItem proxy : proxies) {
                final JsonArrayBuilder interfaces = Json.array();
                interfaces.addAll(proxy.getClasses());
                final JsonObjectBuilder proxyTypeObj = Json.object();
                proxyTypeObj.put("proxy", interfaces);
                final JsonObjectBuilder reflectionEntry = Json.object();
                reflectionEntry.put("type", proxyTypeObj);
                reflectionArray.add(reflectionEntry);
            }
            final JsonObjectBuilder root = Json.object();
            root.put("reflection", reflectionArray);
            root.appendTo(writer);
            proxyConfig.produce(new GeneratedResourceBuildItem(
                    "META-INF/native-image/proxy/reachability-metadata.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
            if (!isGraalVm25OrNewer(nativeImageRunnerBuildItem)) {
                // forces GraalVM/Mandrel 21 to locate the file
                proxyConfig.produce(new GeneratedResourceBuildItem(
                        "META-INF/native-image/proxy/native-image.properties",
                        "Args = -H:+UnlockExperimentalVMOptions -H:ConfigurationResourceRoots=META-INF/native-image/proxy/ -H:-UnlockExperimentalVMOptions\n"
                                .getBytes(StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
